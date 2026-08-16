package com.eventops.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRegistrationRequest(@NotBlank @Size(max = 100) String tokenCancelamento) {
}
