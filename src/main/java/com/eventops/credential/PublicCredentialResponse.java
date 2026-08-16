package com.eventops.credential;

import java.time.Instant;
import java.util.UUID;

public record PublicCredentialResponse(
        UUID inscricaoId,
        String evento,
        Instant inicioEm,
        CredentialStatus status,
        Instant emitidaEm,
        Instant utilizadaEm) {
}
