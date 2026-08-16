package com.eventops.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacoes")
public class NotificationEntity {

    @Id
    private UUID id;
    private String tipo;
    private String destinatario;
    private String assunto;
    private String conteudo;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private int tentativas;
    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;
    @Column(name = "ultimo_erro")
    private String ultimoErro;
    @Column(name = "criada_em")
    private Instant criadaEm;
    @Column(name = "enviada_em")
    private Instant enviadaEm;

    protected NotificationEntity() {
    }

    public NotificationEntity(String tipo, String destinatario, String assunto, String conteudo, Instant agora) {
        this.id = UUID.randomUUID();
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.conteudo = conteudo;
        this.status = NotificationStatus.PENDENTE;
        this.tentativas = 0;
        this.proximaTentativaEm = agora;
        this.criadaEm = agora;
    }

    public void marcarEnviando() {
        this.status = NotificationStatus.ENVIANDO;
    }

    public void marcarEnviada(Instant agora) {
        this.status = NotificationStatus.ENVIADA;
        this.enviadaEm = agora;
        this.ultimoErro = null;
    }

    public void marcarFalha(String erro, Instant agora, int maximoTentativas) {
        this.tentativas++;
        this.ultimoErro = erro == null ? "Falha sem mensagem." : erro.substring(0, Math.min(erro.length(), 2000));
        if (tentativas >= maximoTentativas) {
            this.status = NotificationStatus.FALHA;
            return;
        }
        long minutos = Math.min(60, 1L << Math.min(tentativas, 6));
        this.status = NotificationStatus.PENDENTE;
        this.proximaTentativaEm = agora.plus(Duration.ofMinutes(minutos));
    }

    public UUID getId() { return id; }
    public String getDestinatario() { return destinatario; }
    public String getAssunto() { return assunto; }
    public String getConteudo() { return conteudo; }
    public NotificationStatus getStatus() { return status; }
}
