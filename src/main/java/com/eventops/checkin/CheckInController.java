package com.eventops.checkin;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import com.eventops.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/check-ins")
@SecurityRequirement(name = "oauth2")
public class CheckInController {

    private final CheckInService servico;

    public CheckInController(CheckInService servico) {
        this.servico = servico;
    }

    @PostMapping
    ResponseEntity<CheckInResponse> realizar(
            @PathVariable UUID eventoId,
            @RequestHeader("Idempotency-Key") String chaveIdempotencia,
            @Valid @RequestBody CreateCheckInRequest requisicao) {
        CheckInResult resultado = servico.realizar(eventoId, chaveIdempotencia, requisicao);
        if (resultado.repetida()) {
            return ResponseEntity.ok()
                    .header("Idempotency-Replayed", "true")
                    .body(resultado.checkIn());
        }
        return ResponseEntity.created(URI.create(
                "/api/v1/eventos/" + eventoId + "/check-ins/" + resultado.checkIn().id()))
                .body(resultado.checkIn());
    }

    @GetMapping
    PageResponse<CheckInResponse> listar(@PathVariable UUID eventoId, Pageable pagina) {
        return PageResponse.de(servico.listar(eventoId, pagina));
    }
}
