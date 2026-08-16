package com.eventops.audit.internal;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditorias")
public class AuditEntity {

    @Id
    private UUID id;
    @Column(name = "organizacao_id")
    private UUID organizacaoId;
    private String ator;
    private String acao;
    private String recurso;
    @Column(name = "recurso_id")
    private String recursoId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode dados;
    @Column(name = "id_correlacao")
    private String idCorrelacao;
    @Column(name = "criado_em")
    private Instant criadoEm;

    protected AuditEntity() {
    }

    public AuditEntity(UUID organizacaoId, String ator, String acao, String recurso,
            String recursoId, JsonNode dados, String idCorrelacao, Instant criadoEm) {
        this.id = UUID.randomUUID();
        this.organizacaoId = organizacaoId;
        this.ator = ator;
        this.acao = acao;
        this.recurso = recurso;
        this.recursoId = recursoId;
        this.dados = dados;
        this.idCorrelacao = idCorrelacao;
        this.criadoEm = criadoEm;
    }

    public UUID getId() { return id; }
    public UUID getOrganizacaoId() { return organizacaoId; }
    public String getAtor() { return ator; }
    public String getAcao() { return acao; }
    public String getRecurso() { return recurso; }
    public String getRecursoId() { return recursoId; }
    public JsonNode getDados() { return dados; }
    public String getIdCorrelacao() { return idCorrelacao; }
    public Instant getCriadoEm() { return criadoEm; }
}
