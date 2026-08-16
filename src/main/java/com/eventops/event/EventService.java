package com.eventops.event;

import com.eventops.audit.AuditService;
import com.eventops.event.internal.EventEntity;
import com.eventops.event.internal.EventRepository;
import com.eventops.organization.OrganizationRole;
import com.eventops.organization.OrganizationService;
import com.eventops.shared.Actor;
import com.eventops.shared.BusinessRuleException;
import com.eventops.shared.ConflictException;
import com.eventops.shared.CurrentActor;
import com.eventops.shared.NotFoundException;
import com.eventops.shared.TextNormalizer;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private static final EnumSet<OrganizationRole> GESTORES = EnumSet.of(OrganizationRole.GESTOR_EVENTO);
    private static final EnumSet<OrganizationRole> OPERACAO = EnumSet.of(
            OrganizationRole.GESTOR_EVENTO, OrganizationRole.OPERADOR_CHECKIN);

    private final EventRepository eventos;
    private final OrganizationService organizacoes;
    private final CurrentActor atorAtual;
    private final AuditService auditoria;
    private final Clock relogio;

    public EventService(EventRepository eventos, OrganizationService organizacoes, CurrentActor atorAtual,
            AuditService auditoria, Clock relogio) {
        this.eventos = eventos;
        this.organizacoes = organizacoes;
        this.atorAtual = atorAtual;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public EventResponse criar(CreateEventRequest requisicao) {
        organizacoes.exigirAcesso(requisicao.organizacaoId(), GESTORES);
        Actor ator = atorAtual.obter();
        validarPeriodo(requisicao.inicioEm(), requisicao.fimEm(), requisicao.fusoHorario());
        String slug = slugDisponivel(requisicao.titulo());
        Instant agora = Instant.now(relogio);
        EventEntity evento = eventos.save(new EventEntity(
                requisicao.organizacaoId(), requisicao.titulo().trim(), slug, requisicao.descricao().trim(),
                requisicao.local().trim(), requisicao.fusoHorario(), requisicao.inicioEm(), requisicao.fimEm(),
                requisicao.capacidade(), ator.email(), agora));
        auditoria.registrar(evento.getOrganizacaoId(), ator.email(), "EVENTO_CRIADO", "evento", evento.getId(),
                Map.of("titulo", evento.getTitulo(), "status", evento.getStatus().name()));
        return resposta(evento);
    }

    @Transactional
    public EventResponse atualizar(UUID eventoId, UpdateEventRequest requisicao) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        if (evento.getStatus() != EventStatus.RASCUNHO) {
            throw new ConflictException("EVENTO_NAO_EDITAVEL", "Somente eventos em rascunho podem ser alterados.");
        }
        if (evento.getVersao() != requisicao.versao()) {
            throw new ConflictException("VERSAO_DESATUALIZADA", "O evento foi alterado por outra pessoa.");
        }
        validarPeriodo(requisicao.inicioEm(), requisicao.fimEm(), requisicao.fusoHorario());
        String slug = evento.getTitulo().equals(requisicao.titulo().trim())
                ? evento.getSlug()
                : slugDisponivel(requisicao.titulo());
        evento.atualizar(requisicao.titulo().trim(), slug, requisicao.descricao().trim(), requisicao.local().trim(),
                requisicao.fusoHorario(), requisicao.inicioEm(), requisicao.fimEm(), requisicao.capacidade(),
                Instant.now(relogio));
        Actor ator = atorAtual.obter();
        auditoria.registrar(evento.getOrganizacaoId(), ator.email(), "EVENTO_ATUALIZADO", "evento", evento.getId(),
                Map.of("versaoAnterior", requisicao.versao()));
        eventos.flush();
        return resposta(evento);
    }

    @Transactional
    public EventResponse publicar(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        if (evento.getStatus() != EventStatus.RASCUNHO) {
            throw new ConflictException("EVENTO_JA_PROCESSADO", "O evento nao esta em rascunho.");
        }
        if (!evento.getInicioEm().isAfter(Instant.now(relogio))) {
            throw new BusinessRuleException("INICIO_NO_PASSADO", "Nao e possivel publicar um evento iniciado.");
        }
        evento.publicar(Instant.now(relogio));
        auditarEstado(evento, "EVENTO_PUBLICADO");
        eventos.flush();
        return resposta(evento);
    }

    @Transactional
    public EventResponse encerrarInscricoes(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        if (evento.getStatus() != EventStatus.PUBLICADO) {
            throw new ConflictException("EVENTO_NAO_PUBLICADO", "Somente um evento publicado aceita encerramento.");
        }
        evento.encerrarInscricoes(Instant.now(relogio));
        auditarEstado(evento, "INSCRICOES_ENCERRADAS");
        eventos.flush();
        return resposta(evento);
    }

    @Transactional
    public EventResponse cancelar(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        if (evento.getStatus() == EventStatus.CANCELADO || evento.getStatus() == EventStatus.FINALIZADO) {
            throw new ConflictException("EVENTO_NAO_CANCELAVEL", "O evento nao pode mais ser cancelado.");
        }
        evento.cancelar(Instant.now(relogio));
        auditarEstado(evento, "EVENTO_CANCELADO");
        eventos.flush();
        return resposta(evento);
    }

    @Transactional
    public EventResponse finalizar(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        if (evento.getStatus() != EventStatus.INSCRICOES_ENCERRADAS
                && evento.getStatus() != EventStatus.PUBLICADO) {
            throw new ConflictException("EVENTO_NAO_FINALIZAVEL", "O evento nao esta em operacao.");
        }
        if (evento.getFimEm().isAfter(Instant.now(relogio))) {
            throw new BusinessRuleException("EVENTO_EM_ANDAMENTO", "O evento ainda nao terminou.");
        }
        evento.finalizar(Instant.now(relogio));
        auditarEstado(evento, "EVENTO_FINALIZADO");
        eventos.flush();
        return resposta(evento);
    }

    @Transactional(readOnly = true)
    public EventResponse obterGerenciado(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), OPERACAO);
        return resposta(evento);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listar(UUID organizacaoId, Pageable pagina) {
        organizacoes.exigirAcesso(organizacaoId, OPERACAO);
        return eventos.findByOrganizacaoIdOrderByCriadoEmDesc(organizacaoId, pagina).map(this::resposta);
    }

    @Transactional(readOnly = true)
    public PublicEventResponse obterPublico(String slug) {
        EventEntity evento = eventos.findBySlug(slug)
                .filter(item -> item.getStatus() == EventStatus.PUBLICADO
                        || item.getStatus() == EventStatus.INSCRICOES_ENCERRADAS)
                .orElseThrow(() -> new NotFoundException("EVENTO_NAO_ENCONTRADO", "Evento publico nao encontrado."));
        return respostaPublica(evento);
    }

    @Transactional(readOnly = true)
    public EventSnapshot obterPorSlugParaInscricao(String slug) {
        EventEntity evento = eventos.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("EVENTO_NAO_ENCONTRADO", "Evento nao encontrado."));
        return snapshot(evento);
    }

    @Transactional(readOnly = true)
    public EventSnapshot obterSnapshot(UUID eventoId) {
        return snapshot(obterEntidade(eventoId));
    }

    @Transactional
    public boolean tentarReservarVaga(UUID eventoId) {
        return eventos.reservarVaga(eventoId) == 1;
    }

    @Transactional(readOnly = true)
    public boolean aceitaInscricoes(UUID eventoId) {
        return eventos.aceitaInscricoes(eventoId);
    }

    @Transactional
    public void liberarVaga(UUID eventoId) {
        if (eventos.liberarVaga(eventoId) != 1) {
            throw new NotFoundException("EVENTO_NAO_ENCONTRADO", "Evento nao encontrado para liberar vaga.");
        }
    }

    @Transactional(readOnly = true)
    public UUID exigirOperacao(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), OPERACAO);
        if (evento.getStatus() != EventStatus.PUBLICADO
                && evento.getStatus() != EventStatus.INSCRICOES_ENCERRADAS) {
            throw new ConflictException("EVENTO_FORA_DE_OPERACAO", "O evento nao esta disponivel para check-in.");
        }
        return evento.getOrganizacaoId();
    }

    @Transactional(readOnly = true)
    public UUID exigirGestao(UUID eventoId) {
        EventEntity evento = obterEntidade(eventoId);
        organizacoes.exigirAcesso(evento.getOrganizacaoId(), GESTORES);
        return evento.getOrganizacaoId();
    }

    private EventEntity obterEntidade(UUID eventoId) {
        return eventos.findById(eventoId)
                .orElseThrow(() -> new NotFoundException("EVENTO_NAO_ENCONTRADO", "Evento nao encontrado."));
    }

    private void validarPeriodo(Instant inicio, Instant fim, String fusoHorario) {
        if (!fim.isAfter(inicio)) {
            throw new BusinessRuleException("PERIODO_INVALIDO", "O termino deve ocorrer depois do inicio.");
        }
        try {
            ZoneId.of(fusoHorario);
        } catch (DateTimeException excecao) {
            throw new BusinessRuleException("FUSO_INVALIDO", "Informe um fuso horario IANA valido, como America/Sao_Paulo.");
        }
    }

    private String slugDisponivel(String titulo) {
        String base = TextNormalizer.slug(titulo);
        if (base.isBlank()) {
            throw new BusinessRuleException("TITULO_INVALIDO", "O titulo nao produz um endereco publico valido.");
        }
        return eventos.existsBySlug(base) ? base + "-" + UUID.randomUUID().toString().substring(0, 8) : base;
    }

    private void auditarEstado(EventEntity evento, String acao) {
        Actor ator = atorAtual.obter();
        auditoria.registrar(evento.getOrganizacaoId(), ator.email(), acao, "evento", evento.getId(),
                Map.of("status", evento.getStatus().name()));
    }

    private EventSnapshot snapshot(EventEntity evento) {
        return new EventSnapshot(evento.getId(), evento.getOrganizacaoId(), evento.getTitulo(), evento.getSlug(),
                evento.getInicioEm(), evento.getFimEm(), evento.getCapacidade(), evento.getVagasOcupadas(),
                evento.getStatus());
    }

    private EventResponse resposta(EventEntity evento) {
        Integer disponiveis = evento.getCapacidade() == null ? null : evento.getCapacidade() - evento.getVagasOcupadas();
        return new EventResponse(evento.getId(), evento.getOrganizacaoId(), evento.getTitulo(), evento.getSlug(),
                evento.getDescricao(), evento.getLocal(), evento.getFusoHorario(), evento.getInicioEm(),
                evento.getFimEm(), evento.getCapacidade(), evento.getVagasOcupadas(), disponiveis,
                evento.getStatus(), evento.getVersao(), evento.getCriadoEm(), evento.getAtualizadoEm());
    }

    private PublicEventResponse respostaPublica(EventEntity evento) {
        Integer disponiveis = evento.getCapacidade() == null ? null : evento.getCapacidade() - evento.getVagasOcupadas();
        return new PublicEventResponse(evento.getTitulo(), evento.getSlug(), evento.getDescricao(), evento.getLocal(),
                evento.getFusoHorario(), evento.getInicioEm(), evento.getFimEm(), evento.getCapacidade(),
                evento.getVagasOcupadas(), disponiveis, evento.getStatus());
    }
}
