package com.eventops.event;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/eventos")
@SecurityRequirement(name = "oauth2")
public class EventController {

    private final EventService servico;

    public EventController(EventService servico) {
        this.servico = servico;
    }

    @PostMapping
    ResponseEntity<EventResponse> criar(@Valid @RequestBody CreateEventRequest requisicao) {
        EventResponse resposta = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/eventos/" + resposta.id())).body(resposta);
    }

    @GetMapping("/{eventoId}")
    EventResponse obter(@PathVariable UUID eventoId) {
        return servico.obterGerenciado(eventoId);
    }

    @GetMapping("/organizacao/{organizacaoId}")
    PageResponse<EventResponse> listar(@PathVariable UUID organizacaoId, Pageable pagina) {
        return PageResponse.de(servico.listar(organizacaoId, pagina));
    }

    @PutMapping("/{eventoId}")
    EventResponse atualizar(@PathVariable UUID eventoId, @Valid @RequestBody UpdateEventRequest requisicao) {
        return servico.atualizar(eventoId, requisicao);
    }

    @PostMapping("/{eventoId}/publicacao")
    EventResponse publicar(@PathVariable UUID eventoId) {
        return servico.publicar(eventoId);
    }

    @PostMapping("/{eventoId}/encerramento-inscricoes")
    EventResponse encerrarInscricoes(@PathVariable UUID eventoId) {
        return servico.encerrarInscricoes(eventoId);
    }

    @PostMapping("/{eventoId}/cancelamento")
    EventResponse cancelar(@PathVariable UUID eventoId) {
        return servico.cancelar(eventoId);
    }

    @PostMapping("/{eventoId}/finalizacao")
    EventResponse finalizar(@PathVariable UUID eventoId) {
        return servico.finalizar(eventoId);
    }
}
