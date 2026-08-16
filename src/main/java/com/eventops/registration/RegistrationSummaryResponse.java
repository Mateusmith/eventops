package com.eventops.registration;

import java.time.Instant;
import java.util.UUID;

public record RegistrationSummaryResponse(
        UUID id,
        String nome,
        String email,
        RegistrationStatus status,
        RegistrationOrigin origem,
        Instant confirmadaEm,
        Instant canceladaEm,
        Instant criadoEm) {
}
