package com.eventops.registration.internal;

import com.eventops.registration.WaitingListStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lista_espera")
public class WaitingListEntity {

    @Id
    private UUID id;
    @Column(name = "inscricao_id")
    private UUID inscricaoId;
    @Column(name = "evento_id")
    private UUID eventoId;
    @Enumerated(EnumType.STRING)
    private WaitingListStatus status;
    @Column(name = "entrou_em")
    private Instant entrouEm;
    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    protected WaitingListEntity() {
    }

    public WaitingListEntity(UUID inscricaoId, UUID eventoId, Instant agora) {
        this.id = UUID.randomUUID();
        this.inscricaoId = inscricaoId;
        this.eventoId = eventoId;
        this.status = WaitingListStatus.AGUARDANDO;
        this.entrouEm = agora;
    }

    public void promover(Instant agora) {
        this.status = WaitingListStatus.PROMOVIDA;
        this.finalizadaEm = agora;
    }

    public void cancelar(Instant agora) {
        this.status = WaitingListStatus.CANCELADA;
        this.finalizadaEm = agora;
    }

    public UUID getId() { return id; }
    public UUID getInscricaoId() { return inscricaoId; }
    public UUID getEventoId() { return eventoId; }
    public WaitingListStatus getStatus() { return status; }
    public Instant getEntrouEm() { return entrouEm; }
}
