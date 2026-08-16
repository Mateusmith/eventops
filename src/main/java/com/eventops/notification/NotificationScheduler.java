package com.eventops.notification;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eventops.notificacoes.ativas", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

    private final OutboxClaimService reivindicacao;
    private final NotificationDeliveryService entrega;

    public NotificationScheduler(OutboxClaimService reivindicacao, NotificationDeliveryService entrega) {
        this.reivindicacao = reivindicacao;
        this.entrega = entrega;
    }

    @Scheduled(fixedDelayString = "${eventops.notificacoes.intervalo}")
    void processar() {
        for (UUID eventoId : reivindicacao.reivindicar()) {
            entrega.entregar(eventoId);
        }
    }
}
