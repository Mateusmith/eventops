package com.eventops.event;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publico/eventos")
public class PublicEventController {

    private final EventService servico;

    public PublicEventController(EventService servico) {
        this.servico = servico;
    }

    @GetMapping("/{slug}")
    PublicEventResponse obter(@PathVariable String slug) {
        return servico.obterPublico(slug);
    }
}
