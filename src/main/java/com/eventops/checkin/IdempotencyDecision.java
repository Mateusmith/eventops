package com.eventops.checkin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record IdempotencyDecision(UUID id, boolean nova, int codigoResposta, JsonNode corpoResposta) {
}
