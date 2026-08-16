package com.eventops.registration;

import java.util.UUID;

public record CancellationResponse(UUID inscricaoId, RegistrationStatus status, UUID inscricaoPromovidaId) {
}
