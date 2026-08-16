package com.eventops.registration;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "oauth2")
public class RegistrationController {

    private final RegistrationService servico;

    public RegistrationController(RegistrationService servico) {
        this.servico = servico;
    }

    @PostMapping("/eventos/{eventoId}/convites")
    ResponseEntity<InvitationResponse> criarConvite(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CreateInvitationRequest requisicao) {
        InvitationResponse resposta = servico.criarConvite(eventoId, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/eventos/" + eventoId + "/convites/" + resposta.id()))
                .body(resposta);
    }

    @GetMapping("/eventos/{eventoId}/inscricoes")
    PageResponse<RegistrationSummaryResponse> listar(@PathVariable UUID eventoId, Pageable pagina) {
        return PageResponse.de(servico.listar(eventoId, pagina));
    }

    @PostMapping("/inscricoes/{inscricaoId}/cancelamento")
    CancellationResponse cancelar(@PathVariable UUID inscricaoId) {
        return servico.cancelarPelaOrganizacao(inscricaoId);
    }
}
