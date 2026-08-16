package com.eventops.event;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizacaoId,
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
        EventStatus status,
        long versao,
        Instant criadoEm,
        Instant atualizadoEm) {
}
