package com.eventops.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateEventRequest(
        @NotBlank @Size(max = 180) String titulo,
        @NotBlank @Size(max = 5000) String descricao,
        @NotBlank @Size(max = 220) String local,
        @NotBlank @Size(max = 60) String fusoHorario,
        @NotNull Instant inicioEm,
        @NotNull Instant fimEm,
        @Positive Integer capacidade,
        @NotNull Long versao) {
}
