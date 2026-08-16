package com.eventops.registration;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID eventoId,
        String email,
        InvitationStatus status,
        Instant expiraEm,
        String token,
        String urlAceite) {
}
