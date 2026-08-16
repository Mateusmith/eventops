package com.eventops.credential;

import java.time.Instant;
import java.util.UUID;

public record CredentialInfo(
        UUID id,
        UUID inscricaoId,
        UUID eventoId,
        CredentialStatus status,
        Instant emitidaEm,
        Instant utilizadaEm) {
}
