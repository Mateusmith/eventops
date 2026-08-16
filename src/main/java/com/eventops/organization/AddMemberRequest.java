package com.eventops.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddMemberRequest(
        @NotBlank @Size(max = 160) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull OrganizationRole papel) {
}
