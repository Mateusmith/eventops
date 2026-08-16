package com.eventops.organization.internal;

import com.eventops.organization.OrganizationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membros_organizacao")
public class OrganizationMemberEntity {

    @Id
    private UUID id;
    @Column(name = "organizacao_id")
    private UUID organizacaoId;
    private String nome;
    private String email;
    @Column(name = "email_normalizado")
    private String emailNormalizado;
    @Enumerated(EnumType.STRING)
    private OrganizationRole papel;
    private boolean ativo;
    @Column(name = "criado_em")
    private Instant criadoEm;

    protected OrganizationMemberEntity() {
    }

    public OrganizationMemberEntity(UUID organizacaoId, String nome, String email,
            String emailNormalizado, OrganizationRole papel, Instant agora) {
        this.id = UUID.randomUUID();
        this.organizacaoId = organizacaoId;
        this.nome = nome;
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.papel = papel;
        this.ativo = true;
        this.criadoEm = agora;
    }

    public UUID getId() { return id; }
    public UUID getOrganizacaoId() { return organizacaoId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public OrganizationRole getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
}
