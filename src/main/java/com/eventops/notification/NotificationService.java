package com.eventops.notification;

import com.eventops.notification.internal.NotificationEntity;
import com.eventops.notification.internal.NotificationRepository;
import com.eventops.notification.internal.OutboxEntity;
import com.eventops.notification.internal.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificacoes;
    private final OutboxRepository outbox;
    private final ObjectMapper mapeador;
    private final Clock relogio;

    public NotificationService(NotificationRepository notificacoes, OutboxRepository outbox,
            ObjectMapper mapeador, Clock relogio) {
        this.notificacoes = notificacoes;
        this.outbox = outbox;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    @Transactional
    public UUID agendar(String tipo, String destinatario, String assunto, String conteudo) {
        Instant agora = Instant.now(relogio);
        NotificationEntity notificacao = notificacoes.save(
                new NotificationEntity(tipo, destinatario, assunto, conteudo, agora));
        outbox.save(new OutboxEntity(
                "NOTIFICACAO_SOLICITADA",
                notificacao.getId(),
                mapeador.valueToTree(Map.of("notificacaoId", notificacao.getId(), "tipo", tipo)),
                agora));
        return notificacao.getId();
    }
}
