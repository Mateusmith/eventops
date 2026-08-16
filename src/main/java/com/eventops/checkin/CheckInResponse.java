package com.eventops.checkin;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID inscricaoId,
        UUID eventoId,
        String operador,
        Instant realizadoEm) {
}
