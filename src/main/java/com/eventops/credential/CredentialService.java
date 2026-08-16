package com.eventops.credential;

import com.eventops.audit.AuditService;
import com.eventops.credential.internal.CredentialEntity;
import com.eventops.credential.internal.CredentialRepository;
import com.eventops.event.EventService;
import com.eventops.event.EventSnapshot;
import com.eventops.shared.Actor;
import com.eventops.shared.ConflictException;
import com.eventops.shared.CurrentActor;
import com.eventops.shared.NotFoundException;
import com.eventops.shared.TokenGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CredentialService {

    private final CredentialRepository credenciais;
    private final TokenGenerator tokens;
    private final EventService eventos;
    private final CurrentActor atorAtual;
    private final AuditService auditoria;
    private final Clock relogio;
    private final String urlPublica;

    public CredentialService(CredentialRepository credenciais, TokenGenerator tokens, EventService eventos,
            CurrentActor atorAtual, AuditService auditoria, Clock relogio,
            @Value("${eventops.url-publica}") String urlPublica) {
        this.credenciais = credenciais;
        this.tokens = tokens;
        this.eventos = eventos;
        this.atorAtual = atorAtual;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.urlPublica = urlPublica;
    }

    @Transactional
    public IssuedCredential emitir(UUID inscricaoId, UUID eventoId) {
        if (credenciais.findByInscricaoId(inscricaoId).isPresent()) {
            throw new ConflictException("CREDENCIAL_JA_EXISTE", "A inscricao ja possui uma credencial.");
        }
        String token = tokens.gerar();
        CredentialEntity credencial = credenciais.save(
                new CredentialEntity(inscricaoId, eventoId, tokens.hash(token), Instant.now(relogio)));
        return emitida(credencial, token);
    }

    @Transactional(readOnly = true)
    public PublicCredentialResponse obterPublica(String token) {
        CredentialEntity credencial = buscarToken(token);
        EventSnapshot evento = eventos.obterSnapshot(credencial.getEventoId());
        return new PublicCredentialResponse(credencial.getInscricaoId(), evento.titulo(), evento.inicioEm(),
                credencial.getStatus(), credencial.getEmitidaEm(), credencial.getUtilizadaEm());
    }

    @Transactional(readOnly = true)
    public CredentialInfo validarParaCheckIn(String token) {
        CredentialEntity credencial = buscarToken(token);
        if (credencial.getStatus() != CredentialStatus.ATIVA) {
            throw new ConflictException("CREDENCIAL_INATIVA", "A credencial ja foi utilizada ou revogada.");
        }
        return informacao(credencial);
    }

    @Transactional
    public void marcarUtilizada(UUID credencialId) {
        CredentialEntity credencial = credenciais.findById(credencialId)
                .orElseThrow(() -> new NotFoundException("CREDENCIAL_NAO_ENCONTRADA", "Credencial nao encontrada."));
        if (credencial.getStatus() != CredentialStatus.ATIVA) {
            throw new ConflictException("CREDENCIAL_INATIVA", "A credencial nao esta ativa.");
        }
        credencial.utilizar(Instant.now(relogio));
    }

    @Transactional
    public void revogarPorInscricao(UUID inscricaoId) {
        credenciais.findByInscricaoId(inscricaoId)
                .filter(item -> item.getStatus() == CredentialStatus.ATIVA)
                .ifPresent(item -> item.revogar(Instant.now(relogio)));
    }

    @Transactional
    public IssuedCredential renovar(UUID inscricaoId) {
        CredentialEntity credencial = credenciais.findByInscricaoId(inscricaoId)
                .orElseThrow(() -> new NotFoundException("CREDENCIAL_NAO_ENCONTRADA", "Credencial nao encontrada."));
        UUID organizacaoId = eventos.exigirGestao(credencial.getEventoId());
        String token = tokens.gerar();
        credencial.renovar(tokens.hash(token), Instant.now(relogio));
        Actor ator = atorAtual.obter();
        auditoria.registrar(organizacaoId, ator.email(), "CREDENCIAL_RENOVADA", "credencial",
                credencial.getId(), Map.of("inscricaoId", inscricaoId));
        return emitida(credencial, token);
    }

    private CredentialEntity buscarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new NotFoundException("CREDENCIAL_NAO_ENCONTRADA", "Credencial nao encontrada.");
        }
        return credenciais.findByTokenHash(tokens.hash(token))
                .orElseThrow(() -> new NotFoundException("CREDENCIAL_NAO_ENCONTRADA", "Credencial nao encontrada."));
    }

    private CredentialInfo informacao(CredentialEntity credencial) {
        return new CredentialInfo(credencial.getId(), credencial.getInscricaoId(), credencial.getEventoId(),
                credencial.getStatus(), credencial.getEmitidaEm(), credencial.getUtilizadaEm());
    }

    private IssuedCredential emitida(CredentialEntity credencial, String token) {
        return new IssuedCredential(credencial.getId(), token,
                urlPublica + "/api/v1/publico/credenciais/" + token);
    }
}
