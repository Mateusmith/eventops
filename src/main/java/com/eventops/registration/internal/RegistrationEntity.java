package com.eventops.registration.internal;

import com.eventops.registration.RegistrationOrigin;
import com.eventops.registration.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inscricoes")
public class RegistrationEntity {

    @Id
    private UUID id;
    @Column(name = "evento_id")
    private UUID eventoId;
    private String nome;
    private String email;
    @Column(name = "email_normalizado")
    private String emailNormalizado;
    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;
    @Enumerated(EnumType.STRING)
    private RegistrationOrigin origem;
    @Column(name = "indicador_inscricao_id")
    private UUID indicadorInscricaoId;
    @Column(name = "codigo_indicacao")
    private String codigoIndicacao;
    @Column(name = "token_cancelamento_hash")
    private String tokenCancelamentoHash;
    @Column(name = "confirmada_em")
    private Instant confirmadaEm;
    @Column(name = "cancelada_em")
    private Instant canceladaEm;
    @Column(name = "criado_em")
    private Instant criadoEm;

    protected RegistrationEntity() {
    }

    public RegistrationEntity(UUID eventoId, String nome, String email, String emailNormalizado,
            RegistrationStatus status, RegistrationOrigin origem, UUID indicadorInscricaoId,
            String codigoIndicacao, String tokenCancelamentoHash, Instant agora) {
        this.id = UUID.randomUUID();
        this.eventoId = eventoId;
        this.nome = nome;
        this.email = email;
        this.emailNormalizado = emailNormalizado;
        this.status = status;
        this.origem = origem;
        this.indicadorInscricaoId = indicadorInscricaoId;
        this.codigoIndicacao = codigoIndicacao;
        this.tokenCancelamentoHash = tokenCancelamentoHash;
        this.confirmadaEm = status == RegistrationStatus.CONFIRMADA ? agora : null;
        this.criadoEm = agora;
    }

    public void confirmar(Instant agora) {
        this.status = RegistrationStatus.CONFIRMADA;
        this.confirmadaEm = agora;
    }

    public void cancelar(Instant agora) {
        this.status = RegistrationStatus.CANCELADA;
        this.canceladaEm = agora;
    }

    public UUID getId() { return id; }
    public UUID getEventoId() { return eventoId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getEmailNormalizado() { return emailNormalizado; }
    public RegistrationStatus getStatus() { return status; }
    public RegistrationOrigin getOrigem() { return origem; }
    public UUID getIndicadorInscricaoId() { return indicadorInscricaoId; }
    public String getCodigoIndicacao() { return codigoIndicacao; }
    public String getTokenCancelamentoHash() { return tokenCancelamentoHash; }
    public Instant getConfirmadaEm() { return confirmadaEm; }
    public Instant getCanceladaEm() { return canceladaEm; }
    public Instant getCriadoEm() { return criadoEm; }
}
