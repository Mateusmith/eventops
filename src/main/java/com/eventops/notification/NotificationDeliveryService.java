package com.eventops.notification;

import com.eventops.notification.internal.NotificationEntity;
import com.eventops.notification.internal.NotificationRepository;
import com.eventops.notification.internal.OutboxEntity;
import com.eventops.notification.internal.OutboxRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {

    private final OutboxRepository outbox;
    private final NotificationRepository notificacoes;
    private final JavaMailSender remetente;
    private final Clock relogio;
    private final int maximoTentativas;

    public NotificationDeliveryService(OutboxRepository outbox, NotificationRepository notificacoes,
            JavaMailSender remetente, Clock relogio,
            @Value("${eventops.notificacoes.maximo-tentativas}") int maximoTentativas) {
        this.outbox = outbox;
        this.notificacoes = notificacoes;
        this.remetente = remetente;
        this.relogio = relogio;
        this.maximoTentativas = maximoTentativas;
    }

    @Transactional
    public void entregar(UUID eventoOutboxId) {
        OutboxEntity evento = outbox.findById(eventoOutboxId).orElseThrow();
        NotificationEntity notificacao = notificacoes.findById(evento.getAgregadoId()).orElseThrow();
        notificacao.marcarEnviando();
        try {
            MimeMessage mensagem = remetente.createMimeMessage();
            MimeMessageHelper auxiliar = new MimeMessageHelper(mensagem, "UTF-8");
            auxiliar.setFrom("nao-responda@eventops.local");
            auxiliar.setTo(notificacao.getDestinatario());
            auxiliar.setSubject(notificacao.getAssunto());
            auxiliar.setText(notificacao.getConteudo(), false);
            remetente.send(mensagem);
            Instant agora = Instant.now(relogio);
            notificacao.marcarEnviada(agora);
            evento.marcarProcessado(agora);
        } catch (Exception excecao) {
            Instant agora = Instant.now(relogio);
            notificacao.marcarFalha(excecao.getMessage(), agora, maximoTentativas);
            evento.devolverParaFila(excecao.getMessage(), agora, maximoTentativas);
        }
    }
}
