package com.eventops.organization.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizacoes")
public class OrganizationEntity {

    @Id
    private UUID id;
    private String nome;
    private String slug;
    private String documento;
    private boolean ativa;
    @Column(name = "criado_em")
    private Instant criadoEm;
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    protected OrganizationEntity() {
    }

    public OrganizationEntity(String nome, String slug, String documento, Instant agora) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.slug = slug;
        this.documento = documento;
        this.ativa = true;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getSlug() { return slug; }
    public String getDocumento() { return documento; }
    public boolean isAtiva() { return ativa; }
    public Instant getCriadoEm() { return criadoEm; }
}
