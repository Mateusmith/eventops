package com.eventops.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 160) String nome,
        @Size(max = 30) String documento) {
}
