package com.eventops.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant instante,
        int status,
        String codigo,
        String mensagem,
        String caminho,
        String idCorrelacao,
        List<FieldViolation> violacoes) {

    public record FieldViolation(String campo, String mensagem) {
    }
}
