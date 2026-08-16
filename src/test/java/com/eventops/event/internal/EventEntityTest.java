package com.eventops.event.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventops.event.EventStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEntityTest {

    @Test
    void deveControlarCicloDeVida() {
        Instant agora = Instant.parse("2026-08-16T12:00:00Z");
        EventEntity evento = new EventEntity(
                UUID.randomUUID(), "Java Summit", "java-summit", "Evento", "Sao Paulo",
                "America/Sao_Paulo", agora.plusSeconds(3600), agora.plusSeconds(7200), 100,
                "organizador@eventops.local", agora);

        assertThat(evento.getStatus()).isEqualTo(EventStatus.RASCUNHO);
        evento.publicar(agora.plusSeconds(10));
        assertThat(evento.getStatus()).isEqualTo(EventStatus.PUBLICADO);
        evento.encerrarInscricoes(agora.plusSeconds(20));
        assertThat(evento.getStatus()).isEqualTo(EventStatus.INSCRICOES_ENCERRADAS);
        evento.finalizar(agora.plusSeconds(7300));
        assertThat(evento.getStatus()).isEqualTo(EventStatus.FINALIZADO);
    }
}
