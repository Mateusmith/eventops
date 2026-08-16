package com.eventops.event;

import java.time.Instant;

public record PublicEventResponse(
        String titulo,
        String slug,
        String descricao,
        String local,
        String fusoHorario,
        Instant inicioEm,
        Instant fimEm,
        Integer capacidade,
        int vagasOcupadas,
        Integer vagasDisponiveis,
        EventStatus status) {
}
