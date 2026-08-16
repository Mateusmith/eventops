package com.eventops.registration.internal;

import com.eventops.registration.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convites")
public class InvitationEntity {

    @Id
    private UUID id;
    @Column(name = "evento_id")
    private UUID eventoId;
    private String nome;
    private String email;
    @Column(name = "email_normalizado")
    private String emailNormalizado;
    @Column(name = "token_hash")
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    private InvitationStatus status;
    @Column(name = "expira_em")
    private Instant expiraEm;
    @Column(name = "inscricao_aceita_id")
    private UUID inscricaoAceitaId;
    @Column(name = "criado_por")
    private String criadoPor;
    @Column(name = "criado_em")
    private Instant criadoEm;

    protected InvitationEntity() {
    }

    public InvitationEntity(UUID eventoId, String nome, String email, String emailNormalizado,
            String tokenHash, Instant expiraEm, String criadoPor, Instant agora) {
        this.id = UUID.randomUUID();
        this.eventoId = eventoId;
        this.nome = nome;
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.tokenHash = tokenHash;
        this.status = InvitationStatus.PENDENTE;
        this.expiraEm = expiraEm;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public void aceitar(UUID inscricaoId) {
        this.status = InvitationStatus.ACEITO;
        this.inscricaoAceitaId = inscricaoId;
    }

    public void expirar() { this.status = InvitationStatus.EXPIRADO; }
    public void cancelar() { this.status = InvitationStatus.CANCELADO; }

    public UUID getId() { return id; }
    public UUID getEventoId() { return eventoId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getEmailNormalizado() { return emailNormalizado; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiraEm() { return expiraEm; }
    public UUID getInscricaoAceitaId() { return inscricaoAceitaId; }
    public Instant getCriadoEm() { return criadoEm; }
}
