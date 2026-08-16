package com.eventops.credential;

import java.util.UUID;

public record IssuedCredential(UUID id, String token, String urlCredencial) {
}
