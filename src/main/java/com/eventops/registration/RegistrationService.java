package com.eventops.registration;

import com.eventops.audit.AuditService;
import com.eventops.credential.CredentialService;
import com.eventops.credential.IssuedCredential;
import com.eventops.event.EventService;
import com.eventops.event.EventSnapshot;
import com.eventops.event.EventStatus;
import com.eventops.notification.NotificationService;
import com.eventops.registration.internal.InvitationEntity;
import com.eventops.registration.internal.InvitationRepository;
import com.eventops.registration.internal.RegistrationEntity;
import com.eventops.registration.internal.RegistrationRepository;
import com.eventops.registration.internal.WaitingListEntity;
import com.eventops.registration.internal.WaitingListRepository;
import com.eventops.shared.Actor;
import com.eventops.shared.BusinessRuleException;
import com.eventops.shared.ConflictException;
import com.eventops.shared.CurrentActor;
import com.eventops.shared.NotFoundException;
import com.eventops.shared.TextNormalizer;
import com.eventops.shared.TokenGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final RegistrationRepository inscricoes;
    private final WaitingListRepository listaEspera;
    private final InvitationRepository convites;
    private final EventService eventos;
    private final CredentialService credenciais;
    private final NotificationService notificacoes;
    private final AuditService auditoria;
    private final CurrentActor atorAtual;
    private final TokenGenerator tokens;
    private final JdbcClient jdbc;
    private final Clock relogio;
    private final String urlPublica;

    public RegistrationService(RegistrationRepository inscricoes, WaitingListRepository listaEspera,
            InvitationRepository convites, EventService eventos, CredentialService credenciais,
            NotificationService notificacoes, AuditService auditoria, CurrentActor atorAtual,
            TokenGenerator tokens, JdbcClient jdbc, Clock relogio,
            @Value("${eventops.url-publica}") String urlPublica) {
        this.inscricoes = inscricoes;
        this.listaEspera = listaEspera;
        this.convites = convites;
        this.eventos = eventos;
        this.credenciais = credenciais;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.atorAtual = atorAtual;
        this.tokens = tokens;
        this.jdbc = jdbc;
        this.relogio = relogio;
        this.urlPublica = urlPublica;
    }

    @Transactional
    public RegistrationResponse inscrever(String slug, CreateRegistrationRequest requisicao) {
        EventSnapshot evento = eventos.obterPorSlugParaInscricao(slug);
        validarEventoParaInscricao(evento);
        String emailNormalizado = TextNormalizer.email(requisicao.email());
        if (inscricoes.existsByEventoIdAndEmailNormalizado(evento.id(), emailNormalizado)) {
            throw new ConflictException("INSCRICAO_JA_EXISTE", "Este email ja esta inscrito no evento.");
        }
        if (preenchido(requisicao.tokenConvite()) && preenchido(requisicao.codigoIndicacao())) {
            throw new BusinessRuleException("ORIGEM_AMBIGUA", "Use um convite ou uma indicacao, nunca os dois.");
        }

        InvitationEntity convite = resolverConvite(evento, emailNormalizado, requisicao.tokenConvite());
        RegistrationEntity indicador = resolverIndicador(evento, emailNormalizado, requisicao.codigoIndicacao());
        RegistrationOrigin origem = convite != null ? RegistrationOrigin.CONVITE
                : indicador != null ? RegistrationOrigin.INDICACAO : RegistrationOrigin.DIRETA;

        boolean vagaReservada = eventos.tentarReservarVaga(evento.id());
        if (!vagaReservada && !eventos.aceitaInscricoes(evento.id())) {
            throw new ConflictException("INSCRICOES_FECHADAS", "O evento nao esta aceitando inscricoes.");
        }
        RegistrationStatus status = vagaReservada ? RegistrationStatus.CONFIRMADA : RegistrationStatus.LISTA_ESPERA;
        String tokenCancelamento = tokens.gerar();
        Instant agora = Instant.now(relogio);
        RegistrationEntity inscricao = inscricoes.save(new RegistrationEntity(
                evento.id(), requisicao.nome().trim(), requisicao.email().trim(), emailNormalizado,
                status, origem, indicador == null ? null : indicador.getId(), gerarCodigoIndicacao(),
                tokens.hash(tokenCancelamento), agora));

        IssuedCredential credencial = null;
        Long posicao = null;
        if (status == RegistrationStatus.CONFIRMADA) {
            credencial = credenciais.emitir(inscricao.getId(), evento.id());
        } else {
            listaEspera.save(new WaitingListEntity(inscricao.getId(), evento.id(), agora));
            posicao = listaEspera.countByEventoIdAndStatus(evento.id(), WaitingListStatus.AGUARDANDO);
        }
        if (convite != null) {
            convite.aceitar(inscricao.getId());
        }

        agendarConfirmacao(inscricao, evento, credencial, posicao);
        auditoria.registrar(evento.organizacaoId(), emailNormalizado, "INSCRICAO_CRIADA", "inscricao",
                inscricao.getId(), Map.of("status", status.name(), "origem", origem.name()));
        return respostaCriacao(inscricao, tokenCancelamento, posicao, credencial);
    }

    @Transactional
    public InvitationResponse criarConvite(UUID eventoId, CreateInvitationRequest requisicao) {
        UUID organizacaoId = eventos.exigirGestao(eventoId);
        EventSnapshot evento = eventos.obterSnapshot(eventoId);
        if (evento.status() == EventStatus.CANCELADO || evento.status() == EventStatus.FINALIZADO) {
            throw new ConflictException("EVENTO_NAO_ACEITA_CONVITES", "O evento nao aceita novos convites.");
        }
        String emailNormalizado = TextNormalizer.email(requisicao.email());
        if (convites.existsByEventoIdAndEmailNormalizadoAndStatus(eventoId, emailNormalizado, InvitationStatus.PENDENTE)) {
            throw new ConflictException("CONVITE_JA_EXISTE", "Ja existe um convite pendente para este email.");
        }
        Actor ator = atorAtual.obter();
        String token = tokens.gerar();
        InvitationEntity convite = convites.save(new InvitationEntity(
                eventoId, requisicao.nome(), requisicao.email().trim(), emailNormalizado,
                tokens.hash(token), requisicao.expiraEm(), ator.email(), Instant.now(relogio)));
        String url = urlPublica + "/api/v1/publico/convites/" + token;
        notificacoes.agendar("CONVITE_EVENTO", convite.getEmail(), "Convite para " + evento.titulo(),
                "Voce recebeu um convite para " + evento.titulo() + ".\nAcesse: " + url);
        auditoria.registrar(organizacaoId, ator.email(), "CONVITE_CRIADO", "convite", convite.getId(),
                Map.of("email", emailNormalizado, "eventoId", eventoId));
        return new InvitationResponse(convite.getId(), eventoId, convite.getEmail(), convite.getStatus(),
                convite.getExpiraEm(), token, url);
    }

    @Transactional
    public PublicInvitationResponse obterConvite(String token) {
        InvitationEntity convite = buscarConvite(token);
        if (convite.getStatus() == InvitationStatus.PENDENTE
                && !convite.getExpiraEm().isAfter(Instant.now(relogio))) {
            convite.expirar();
        }
        EventSnapshot evento = eventos.obterSnapshot(convite.getEventoId());
        return new PublicInvitationResponse(evento.titulo(), evento.slug(), protegerEmail(convite.getEmail()),
                convite.getStatus(), convite.getExpiraEm());
    }

    @Transactional
    public CancellationResponse cancelarPublico(UUID inscricaoId, CancelRegistrationRequest requisicao) {
        RegistrationEntity inscricao = buscarParaAtualizar(inscricaoId);
        if (!hashIgual(inscricao.getTokenCancelamentoHash(), tokens.hash(requisicao.tokenCancelamento()))) {
            throw new NotFoundException("INSCRICAO_NAO_ENCONTRADA", "Inscricao ou token de cancelamento invalido.");
        }
        return cancelar(inscricao, inscricao.getEmailNormalizado());
    }

    @Transactional
    public CancellationResponse cancelarPelaOrganizacao(UUID inscricaoId) {
        RegistrationEntity inscricao = buscarParaAtualizar(inscricaoId);
        eventos.exigirGestao(inscricao.getEventoId());
        return cancelar(inscricao, atorAtual.obter().email());
    }

    @Transactional(readOnly = true)
    public Page<RegistrationSummaryResponse> listar(UUID eventoId, Pageable pagina) {
        eventos.exigirOperacao(eventoId);
        return inscricoes.findByEventoIdOrderByCriadoEmDesc(eventoId, pagina).map(this::respostaResumo);
    }

    @Transactional(readOnly = true)
    public List<RankingItemResponse> ranking(String slug) {
        eventos.obterPublico(slug);
        EventSnapshot evento = eventos.obterPorSlugParaInscricao(slug);
        List<RankingItemResponse> itens = new ArrayList<>();
        jdbc.sql("""
                SELECT indicador.id AS inscricao_id,
                       indicador.nome AS participante,
                       COUNT(indicada.id) AS quantidade
                  FROM inscricoes indicada
                  JOIN inscricoes indicador ON indicador.id = indicada.indicador_inscricao_id
                 WHERE indicada.evento_id = :eventoId
                   AND indicada.status = 'CONFIRMADA'
                   AND indicador.status = 'CONFIRMADA'
                 GROUP BY indicador.id, indicador.nome, indicador.criado_em
                 ORDER BY quantidade DESC, indicador.criado_em ASC
                 LIMIT 10
                """)
                .param("eventoId", evento.id())
                .query((resultado, numeroLinha) -> new Object[] {
                        resultado.getObject("inscricao_id", UUID.class),
                        resultado.getString("participante"),
                        resultado.getLong("quantidade")
                })
                .list()
                .forEach(linha -> itens.add(new RankingItemResponse(
                        itens.size() + 1, (UUID) linha[0], protegerNome((String) linha[1]), (Long) linha[2])));
        return itens;
    }

    private CancellationResponse cancelar(RegistrationEntity inscricao, String ator) {
        if (inscricao.getStatus() == RegistrationStatus.CANCELADA) {
            return new CancellationResponse(inscricao.getId(), inscricao.getStatus(), null);
        }
        EventSnapshot evento = eventos.obterSnapshot(inscricao.getEventoId());
        RegistrationStatus statusAnterior = inscricao.getStatus();
        Instant agora = Instant.now(relogio);
        inscricao.cancelar(agora);
        credenciais.revogarPorInscricao(inscricao.getId());

        UUID promovidaId = null;
        if (statusAnterior == RegistrationStatus.CONFIRMADA) {
            eventos.liberarVaga(evento.id());
            promovidaId = promoverPrimeiraDaFila(evento);
        } else {
            listaEspera.findByInscricaoId(inscricao.getId()).ifPresent(item -> item.cancelar(agora));
        }
        notificacoes.agendar("INSCRICAO_CANCELADA", inscricao.getEmail(), "Inscricao cancelada",
                "Sua inscricao no evento " + evento.titulo() + " foi cancelada.");
        auditoria.registrar(evento.organizacaoId(), ator, "INSCRICAO_CANCELADA", "inscricao", inscricao.getId(),
                Map.of("statusAnterior", statusAnterior.name(), "promocaoGerada", promovidaId != null));
        return new CancellationResponse(inscricao.getId(), inscricao.getStatus(), promovidaId);
    }

    private UUID promoverPrimeiraDaFila(EventSnapshot evento) {
        List<WaitingListEntity> fila = listaEspera.buscarPrimeirosParaPromocao(evento.id(), PageRequest.of(0, 1));
        if (fila.isEmpty() || !eventos.tentarReservarVaga(evento.id())) {
            return null;
        }
        WaitingListEntity item = fila.getFirst();
        RegistrationEntity promovida = buscarParaAtualizar(item.getInscricaoId());
        Instant agora = Instant.now(relogio);
        promovida.confirmar(agora);
        item.promover(agora);
        IssuedCredential credencial = credenciais.emitir(promovida.getId(), evento.id());
        notificacoes.agendar("LISTA_ESPERA_PROMOVIDA", promovida.getEmail(),
                "Sua vaga em " + evento.titulo() + " foi confirmada",
                "Uma vaga foi liberada. Sua inscricao esta confirmada.\nCredencial: " + credencial.urlCredencial());
        auditoria.registrar(evento.organizacaoId(), "sistema", "LISTA_ESPERA_PROMOVIDA", "inscricao",
                promovida.getId(), Map.of("eventoId", evento.id()));
        return promovida.getId();
    }

    private InvitationEntity resolverConvite(EventSnapshot evento, String email, String tokenConvite) {
        if (!preenchido(tokenConvite)) {
            return null;
        }
        InvitationEntity convite = buscarConvite(tokenConvite);
        if (convite.getStatus() != InvitationStatus.PENDENTE) {
            throw new ConflictException("CONVITE_INATIVO", "O convite nao esta mais disponivel.");
        }
        if (!convite.getExpiraEm().isAfter(Instant.now(relogio))) {
            convite.expirar();
            throw new ConflictException("CONVITE_EXPIRADO", "O convite expirou.");
        }
        if (!convite.getEventoId().equals(evento.id()) || !convite.getEmailNormalizado().equals(email)) {
            throw new ConflictException("CONVITE_INCOMPATIVEL", "O convite nao pertence a este evento ou email.");
        }
        return convite;
    }

    private RegistrationEntity resolverIndicador(EventSnapshot evento, String email, String codigoIndicacao) {
        if (!preenchido(codigoIndicacao)) {
            return null;
        }
        RegistrationEntity indicador = inscricoes.findByEventoIdAndCodigoIndicacaoAndStatus(
                        evento.id(), codigoIndicacao.trim(), RegistrationStatus.CONFIRMADA)
                .orElseThrow(() -> new NotFoundException("INDICACAO_NAO_ENCONTRADA", "Codigo de indicacao invalido."));
        if (indicador.getEmailNormalizado().equals(email)) {
            throw new BusinessRuleException("AUTOINDICACAO", "Uma pessoa nao pode indicar a si mesma.");
        }
        return indicador;
    }

    private InvitationEntity buscarConvite(String token) {
        if (!preenchido(token)) {
            throw new NotFoundException("CONVITE_NAO_ENCONTRADO", "Convite nao encontrado.");
        }
        return convites.findByTokenHash(tokens.hash(token))
                .orElseThrow(() -> new NotFoundException("CONVITE_NAO_ENCONTRADO", "Convite nao encontrado."));
    }

    private RegistrationEntity buscarParaAtualizar(UUID inscricaoId) {
        return inscricoes.buscarParaAtualizar(inscricaoId)
                .orElseThrow(() -> new NotFoundException("INSCRICAO_NAO_ENCONTRADA", "Inscricao nao encontrada."));
    }

    private void validarEventoParaInscricao(EventSnapshot evento) {
        if (evento.status() != EventStatus.PUBLICADO) {
            throw new ConflictException("INSCRICOES_FECHADAS", "O evento nao esta aceitando inscricoes.");
        }
        if (!evento.inicioEm().isAfter(Instant.now(relogio))) {
            throw new ConflictException("EVENTO_INICIADO", "O evento ja iniciou.");
        }
    }

    private void agendarConfirmacao(RegistrationEntity inscricao, EventSnapshot evento,
            IssuedCredential credencial, Long posicao) {
        if (inscricao.getStatus() == RegistrationStatus.CONFIRMADA) {
            notificacoes.agendar("INSCRICAO_CONFIRMADA", inscricao.getEmail(),
                    "Inscricao confirmada em " + evento.titulo(),
                    "Sua vaga esta confirmada.\nCredencial: " + credencial.urlCredencial()
                            + "\nCodigo de indicacao: " + inscricao.getCodigoIndicacao());
        } else {
            notificacoes.agendar("LISTA_ESPERA", inscricao.getEmail(),
                    "Voce esta na lista de espera de " + evento.titulo(),
                    "Sua posicao atual na lista de espera e " + posicao + ".");
        }
    }

    private RegistrationResponse respostaCriacao(RegistrationEntity inscricao, String tokenCancelamento,
            Long posicao, IssuedCredential credencial) {
        return new RegistrationResponse(inscricao.getId(), inscricao.getNome(), inscricao.getEmail(),
                inscricao.getStatus(), inscricao.getOrigem(), inscricao.getCodigoIndicacao(),
                tokenCancelamento, posicao, credencial, inscricao.getCriadoEm());
    }

    private RegistrationSummaryResponse respostaResumo(RegistrationEntity inscricao) {
        return new RegistrationSummaryResponse(inscricao.getId(), inscricao.getNome(), inscricao.getEmail(),
                inscricao.getStatus(), inscricao.getOrigem(), inscricao.getConfirmadaEm(),
                inscricao.getCanceladaEm(), inscricao.getCriadoEm());
    }

    private String gerarCodigoIndicacao() {
        String codigo;
        do {
            codigo = tokens.gerar().substring(0, 12).toUpperCase(Locale.ROOT);
        } while (inscricoes.existsByCodigoIndicacao(codigo));
        return codigo;
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean hashIgual(String esperado, String recebido) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }

    private String protegerEmail(String email) {
        int separador = email.indexOf('@');
        if (separador <= 1) {
            return "***" + email.substring(Math.max(0, separador));
        }
        return email.substring(0, 1) + "***" + email.substring(separador);
    }

    private String protegerNome(String nome) {
        String[] partes = nome.trim().split("\\s+");
        return partes.length == 1 ? partes[0] : partes[0] + " " + partes[partes.length - 1].charAt(0) + ".";
    }
}
