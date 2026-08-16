package com.eventops.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCheckInRequest(@NotBlank @Size(max = 100) String tokenCredencial) {
}
