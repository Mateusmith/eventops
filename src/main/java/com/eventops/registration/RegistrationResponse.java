package com.eventops.registration;

import com.eventops.credential.IssuedCredential;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        String nome,
        String email,
        RegistrationStatus status,
        RegistrationOrigin origem,
        String codigoIndicacao,
        String tokenCancelamento,
        Long posicaoListaEspera,
        IssuedCredential credencial,
        Instant criadoEm) {
}
