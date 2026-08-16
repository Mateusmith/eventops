package com.eventops.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRegistrationRequest(
        @NotBlank @Size(max = 160) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 100) String tokenConvite,
        @Size(max = 24) String codigoIndicacao) {
}
