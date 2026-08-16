package com.eventops.notification.internal;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "eventos_outbox")
public class OutboxEntity {

    @Id
    private UUID id;
    private String tipo;
    @Column(name = "agregado_id")
    private UUID agregadoId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private int tentativas;
    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;
    @Column(name = "criado_em")
    private Instant criadoEm;
    @Column(name = "processado_em")
    private Instant processadoEm;
    @Column(name = "ultimo_erro")
    private String ultimoErro;
    @Column(name = "bloqueado_em")
    private Instant bloqueadoEm;

    protected OutboxEntity() {
    }

    public OutboxEntity(String tipo, UUID agregadoId, JsonNode payload, Instant agora) {
        this.id = UUID.randomUUID();
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.status = OutboxStatus.PENDENTE;
        this.tentativas = 0;
        this.proximaTentativaEm = agora;
        this.criadoEm = agora;
    }

    public void marcarProcessando(Instant agora) {
        this.status = OutboxStatus.PROCESSANDO;
        this.bloqueadoEm = agora;
    }

    public void marcarProcessado(Instant agora) {
        this.status = OutboxStatus.PROCESSADO;
        this.processadoEm = agora;
        this.ultimoErro = null;
        this.bloqueadoEm = null;
    }

    public void devolverParaFila(String erro, Instant agora, int maximoTentativas) {
        this.tentativas++;
        this.ultimoErro = erro == null ? "Falha sem mensagem." : erro.substring(0, Math.min(erro.length(), 2000));
        this.bloqueadoEm = null;
        if (tentativas >= maximoTentativas) {
            this.status = OutboxStatus.FALHA;
            return;
        }
        long minutos = Math.min(60, 1L << Math.min(tentativas, 6));
        this.status = OutboxStatus.PENDENTE;
        this.proximaTentativaEm = agora.plus(Duration.ofMinutes(minutos));
    }

    public UUID getId() { return id; }
    public UUID getAgregadoId() { return agregadoId; }
}
