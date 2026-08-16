package com.eventops.event;

import java.time.Instant;
import java.util.UUID;

public record EventSnapshot(
        UUID id,
        UUID organizacaoId,
        String titulo,
        String slug,
        Instant inicioEm,
        Instant fimEm,
        Integer capacidade,
        int vagasOcupadas,
        EventStatus status) {
}
