package com.eventops.notification;

import com.eventops.notification.internal.OutboxEntity;
import com.eventops.notification.internal.OutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxClaimService {

    private final OutboxRepository repositorio;
    private final Clock relogio;

    public OutboxClaimService(OutboxRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    @Transactional
    public List<UUID> reivindicar() {
        Instant agora = Instant.now(relogio);
        List<OutboxEntity> eventos = repositorio.buscarPendentes(agora, agora.minus(Duration.ofMinutes(5)));
        eventos.forEach(item -> item.marcarProcessando(agora));
        return eventos.stream().map(OutboxEntity::getId).toList();
    }
}
