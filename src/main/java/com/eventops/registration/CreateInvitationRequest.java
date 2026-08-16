package com.eventops.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateInvitationRequest(
        @Size(max = 160) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Future Instant expiraEm) {
}
