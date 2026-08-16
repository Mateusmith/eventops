package com.eventops.credential.internal;

import com.eventops.credential.CredentialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credenciais")
public class CredentialEntity {

    @Id
    private UUID id;
    @Column(name = "inscricao_id")
    private UUID inscricaoId;
    @Column(name = "evento_id")
    private UUID eventoId;
    @Column(name = "token_hash")
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    private CredentialStatus status;
    @Column(name = "emitida_em")
    private Instant emitidaEm;
    @Column(name = "utilizada_em")
    private Instant utilizadaEm;
    @Column(name = "revogada_em")
    private Instant revogadaEm;

    protected CredentialEntity() {
    }

    public CredentialEntity(UUID inscricaoId, UUID eventoId, String tokenHash, Instant agora) {
        this.id = UUID.randomUUID();
        this.inscricaoId = inscricaoId;
        this.eventoId = eventoId;
        this.tokenHash = tokenHash;
        this.status = CredentialStatus.ATIVA;
        this.emitidaEm = agora;
    }

    public void utilizar(Instant agora) {
        this.status = CredentialStatus.UTILIZADA;
        this.utilizadaEm = agora;
    }

    public void revogar(Instant agora) {
        this.status = CredentialStatus.REVOGADA;
        this.revogadaEm = agora;
    }

    public void renovar(String novoHash, Instant agora) {
        this.tokenHash = novoHash;
        this.status = CredentialStatus.ATIVA;
        this.emitidaEm = agora;
        this.utilizadaEm = null;
        this.revogadaEm = null;
    }

    public UUID getId() { return id; }
    public UUID getInscricaoId() { return inscricaoId; }
    public UUID getEventoId() { return eventoId; }
    public CredentialStatus getStatus() { return status; }
    public Instant getEmitidaEm() { return emitidaEm; }
    public Instant getUtilizadaEm() { return utilizadaEm; }
}
