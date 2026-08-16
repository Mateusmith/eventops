package com.eventops.checkin;

import com.eventops.audit.AuditService;
import com.eventops.checkin.internal.CheckInEntity;
import com.eventops.checkin.internal.CheckInRepository;
import com.eventops.credential.CredentialInfo;
import com.eventops.credential.CredentialService;
import com.eventops.event.EventService;
import com.eventops.shared.Actor;
import com.eventops.shared.ConflictException;
import com.eventops.shared.CurrentActor;
import com.eventops.shared.TokenGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInService {

    private final CheckInRepository checkIns;
    private final CredentialService credenciais;
    private final EventService eventos;
    private final IdempotencyService idempotencia;
    private final CurrentActor atorAtual;
    private final AuditService auditoria;
    private final TokenGenerator hashes;
    private final ObjectMapper mapeador;
    private final Clock relogio;
    private final Counter realizados;
    private final Counter repetidos;

    public CheckInService(CheckInRepository checkIns, CredentialService credenciais, EventService eventos,
            IdempotencyService idempotencia, CurrentActor atorAtual, AuditService auditoria,
            TokenGenerator hashes, ObjectMapper mapeador, Clock relogio, MeterRegistry metricas) {
        this.checkIns = checkIns;
        this.credenciais = credenciais;
        this.eventos = eventos;
        this.idempotencia = idempotencia;
        this.atorAtual = atorAtual;
        this.auditoria = auditoria;
        this.hashes = hashes;
        this.mapeador = mapeador;
        this.relogio = relogio;
        this.realizados = metricas.counter("eventops.checkins", "resultado", "realizado");
        this.repetidos = metricas.counter("eventops.checkins", "resultado", "repetido");
    }

    @Transactional
    public CheckInResult realizar(UUID eventoId, String chaveIdempotencia, CreateCheckInRequest requisicao) {
        Actor ator = atorAtual.obter();
        UUID organizacaoId = eventos.exigirOperacao(eventoId);
        String hashRequisicao = hashes.hash(eventoId + ":" + requisicao.tokenCredencial());
        IdempotencyDecision decisao = idempotencia.iniciar(
                ator.id(), "CHECK_IN", chaveIdempotencia, hashRequisicao);
        if (!decisao.nova()) {
            repetidos.increment();
            return new CheckInResult(mapeador.convertValue(decisao.corpoResposta(), CheckInResponse.class), true);
        }

        CredentialInfo credencial = credenciais.validarParaCheckIn(requisicao.tokenCredencial());
        if (!eventoId.equals(credencial.eventoId())) {
            throw new ConflictException("CREDENCIAL_DE_OUTRO_EVENTO", "A credencial pertence a outro evento.");
        }

        if (checkIns.findByInscricaoId(credencial.inscricaoId()).isPresent()) {
            throw new ConflictException("CHECK_IN_JA_REALIZADO", "A inscricao ja realizou check-in.");
        }
        CheckInEntity checkIn = checkIns.saveAndFlush(new CheckInEntity(
                credencial.inscricaoId(), credencial.id(), eventoId, ator.email(), Instant.now(relogio)));
        credenciais.marcarUtilizada(credencial.id());
        CheckInResponse resposta = resposta(checkIn);
        idempotencia.concluir(decisao.id(), 201, mapeador.valueToTree(resposta));
        auditoria.registrar(organizacaoId, ator.email(), "CHECK_IN_REALIZADO", "check_in", checkIn.getId(),
                Map.of("inscricaoId", checkIn.getInscricaoId(), "eventoId", eventoId));
        realizados.increment();
        return new CheckInResult(resposta, false);
    }

    @Transactional(readOnly = true)
    public Page<CheckInResponse> listar(UUID eventoId, Pageable pagina) {
        eventos.exigirOperacao(eventoId);
        return checkIns.findByEventoIdOrderByRealizadoEmDesc(eventoId, pagina).map(this::resposta);
    }

    private CheckInResponse resposta(CheckInEntity entidade) {
        return new CheckInResponse(entidade.getId(), entidade.getInscricaoId(), entidade.getEventoId(),
                entidade.getOperador(), entidade.getRealizadoEm());
    }
}
