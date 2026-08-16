package com.eventops.checkin.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckInEntity {

    @Id
    private UUID id;
    @Column(name = "inscricao_id")
    private UUID inscricaoId;
    @Column(name = "credencial_id")
    private UUID credencialId;
    @Column(name = "evento_id")
    private UUID eventoId;
    private String operador;
    @Column(name = "realizado_em")
    private Instant realizadoEm;

    protected CheckInEntity() {
    }

    public CheckInEntity(UUID inscricaoId, UUID credencialId, UUID eventoId, String operador, Instant agora) {
        this.id = UUID.randomUUID();
        this.inscricaoId = inscricaoId;
        this.credencialId = credencialId;
        this.eventoId = eventoId;
        this.operador = operador;
        this.realizadoEm = agora;
    }

    public UUID getId() { return id; }
    public UUID getInscricaoId() { return inscricaoId; }
    public UUID getCredencialId() { return credencialId; }
    public UUID getEventoId() { return eventoId; }
    public String getOperador() { return operador; }
    public Instant getRealizadoEm() { return realizadoEm; }
}
