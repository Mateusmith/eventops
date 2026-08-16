package com.eventops.audit;

import com.eventops.audit.internal.AuditEntity;
import com.eventops.audit.internal.AuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditRepository repositorio;
    private final ObjectMapper mapeador;
    private final Clock relogio;

    public AuditService(AuditRepository repositorio, ObjectMapper mapeador, Clock relogio) {
        this.repositorio = repositorio;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    public void registrar(UUID organizacaoId, String ator, String acao, String recurso,
            Object recursoId, Map<String, ?> dados) {
        repositorio.save(new AuditEntity(
                organizacaoId,
                ator,
                acao,
                recurso,
                String.valueOf(recursoId),
                mapeador.valueToTree(dados),
                MDC.get("idCorrelacao"),
                Instant.now(relogio)));
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> listar(UUID organizacaoId, Pageable pagina) {
        return repositorio.findByOrganizacaoIdOrderByCriadoEmDesc(organizacaoId, pagina)
                .map(item -> new AuditResponse(item.getId(), item.getAtor(), item.getAcao(), item.getRecurso(),
                        item.getRecursoId(), item.getDados(), item.getIdCorrelacao(), item.getCriadoEm()));
    }
}
