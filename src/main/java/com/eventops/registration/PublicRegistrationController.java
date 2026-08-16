package com.eventops.registration;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico")
public class PublicRegistrationController {

    private final RegistrationService servico;

    public PublicRegistrationController(RegistrationService servico) {
        this.servico = servico;
    }

    @PostMapping("/eventos/{slug}/inscricoes")
    ResponseEntity<RegistrationResponse> inscrever(
            @PathVariable String slug,
            @Valid @RequestBody CreateRegistrationRequest requisicao) {
        RegistrationResponse resposta = servico.inscrever(slug, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/publico/inscricoes/" + resposta.id())).body(resposta);
    }

    @GetMapping("/eventos/{slug}/ranking")
    List<RankingItemResponse> ranking(@PathVariable String slug) {
        return servico.ranking(slug);
    }

    @GetMapping("/convites/{token}")
    PublicInvitationResponse obterConvite(@PathVariable String token) {
        return servico.obterConvite(token);
    }

    @PostMapping("/inscricoes/{inscricaoId}/cancelamento")
    CancellationResponse cancelar(
            @PathVariable UUID inscricaoId,
            @Valid @RequestBody CancelRegistrationRequest requisicao) {
        return servico.cancelarPublico(inscricaoId, requisicao);
    }
}
