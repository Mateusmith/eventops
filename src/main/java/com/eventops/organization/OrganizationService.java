package com.eventops.organization;

import com.eventops.audit.AuditService;
import com.eventops.organization.internal.OrganizationEntity;
import com.eventops.organization.internal.OrganizationMemberEntity;
import com.eventops.organization.internal.OrganizationMemberRepository;
import com.eventops.organization.internal.OrganizationRepository;
import com.eventops.shared.Actor;
import com.eventops.shared.ConflictException;
import com.eventops.shared.CurrentActor;
import com.eventops.shared.ForbiddenException;
import com.eventops.shared.NotFoundException;
import com.eventops.shared.TextNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizacoes;
    private final OrganizationMemberRepository membros;
    private final CurrentActor atorAtual;
    private final AuditService auditoria;
    private final Clock relogio;

    public OrganizationService(OrganizationRepository organizacoes, OrganizationMemberRepository membros,
            CurrentActor atorAtual, AuditService auditoria, Clock relogio) {
        this.organizacoes = organizacoes;
        this.membros = membros;
        this.atorAtual = atorAtual;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public OrganizationResponse criar(CreateOrganizationRequest requisicao) {
        Actor ator = atorAtual.obter();
        String baseSlug = TextNormalizer.slug(requisicao.nome());
        String slug = organizacoes.existsBySlug(baseSlug)
                ? baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8)
                : baseSlug;
        Instant agora = Instant.now(relogio);
        OrganizationEntity organizacao = organizacoes.save(
                new OrganizationEntity(requisicao.nome().trim(), slug, requisicao.documento(), agora));
        membros.save(new OrganizationMemberEntity(
                organizacao.getId(), ator.nome(), ator.email(), TextNormalizer.email(ator.email()),
                OrganizationRole.PROPRIETARIO, agora));
        auditoria.registrar(organizacao.getId(), ator.email(), "ORGANIZACAO_CRIADA", "organizacao",
                organizacao.getId(), Map.of("nome", organizacao.getNome()));
        return resposta(organizacao);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listarMinhas() {
        Actor ator = atorAtual.obter();
        return membros.findByEmailNormalizadoAndAtivoTrueOrderByCriadoEmDesc(TextNormalizer.email(ator.email()))
                .stream()
                .map(OrganizationMemberEntity::getOrganizacaoId)
                .map(id -> organizacoes.findByIdAndAtivaTrue(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::resposta)
                .toList();
    }

    @Transactional
    public MemberResponse adicionarMembro(UUID organizacaoId, AddMemberRequest requisicao) {
        Actor ator = atorAtual.obter();
        exigirAcesso(organizacaoId, EnumSet.of(OrganizationRole.PROPRIETARIO));
        String emailNormalizado = TextNormalizer.email(requisicao.email());
        if (membros.existsByOrganizacaoIdAndEmailNormalizado(organizacaoId, emailNormalizado)) {
            throw new ConflictException("MEMBRO_JA_EXISTE", "Este email ja pertence a organizacao.");
        }
        OrganizationMemberEntity membro = membros.save(new OrganizationMemberEntity(
                organizacaoId, requisicao.nome().trim(), requisicao.email().trim(), emailNormalizado,
                requisicao.papel(), Instant.now(relogio)));
        auditoria.registrar(organizacaoId, ator.email(), "MEMBRO_ADICIONADO", "membro",
                membro.getId(), Map.of("email", membro.getEmail(), "papel", membro.getPapel().name()));
        return new MemberResponse(membro.getId(), membro.getNome(), membro.getEmail(), membro.getPapel(), membro.isAtivo());
    }

    @Transactional(readOnly = true)
    public OrganizationRole exigirAcesso(UUID organizacaoId, Set<OrganizationRole> papeisPermitidos) {
        organizacoes.findByIdAndAtivaTrue(organizacaoId)
                .orElseThrow(() -> new NotFoundException("ORGANIZACAO_NAO_ENCONTRADA", "Organizacao nao encontrada."));
        Actor ator = atorAtual.obter();
        OrganizationMemberEntity membro = membros
                .findByOrganizacaoIdAndEmailNormalizadoAndAtivoTrue(organizacaoId, TextNormalizer.email(ator.email()))
                .orElseThrow(() -> new ForbiddenException("SEM_ACESSO_ORGANIZACAO", "Voce nao pertence a esta organizacao."));
        if (membro.getPapel() != OrganizationRole.PROPRIETARIO && !papeisPermitidos.contains(membro.getPapel())) {
            throw new ForbiddenException("PAPEL_INSUFICIENTE", "Seu papel nao permite esta operacao.");
        }
        return membro.getPapel();
    }

    private OrganizationResponse resposta(OrganizationEntity entidade) {
        return new OrganizationResponse(entidade.getId(), entidade.getNome(), entidade.getSlug(),
                entidade.getDocumento(), entidade.isAtiva(), entidade.getCriadoEm());
    }
}
