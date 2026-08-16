package com.eventops.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        String ator,
        String acao,
        String recurso,
        String recursoId,
        JsonNode dados,
        String idCorrelacao,
        Instant criadoEm) {
}
