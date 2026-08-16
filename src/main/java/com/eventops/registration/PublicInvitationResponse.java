package com.eventops.registration;

import java.time.Instant;

public record PublicInvitationResponse(
        String evento,
        String slugEvento,
        String emailProtegido,
        InvitationStatus status,
        Instant expiraEm) {
}
