package com.eventops.organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String nome,
        String slug,
        String documento,
        boolean ativa,
        Instant criadoEm) {
}
