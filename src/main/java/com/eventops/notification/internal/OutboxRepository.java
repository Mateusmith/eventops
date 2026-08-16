package com.eventops.notification.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query(value = """
            SELECT * FROM eventos_outbox
             WHERE (status = 'PENDENTE' AND proxima_tentativa_em <= :agora)
                OR (status = 'PROCESSANDO' AND bloqueado_em < :limiteRecuperacao)
             ORDER BY criado_em
             FOR UPDATE SKIP LOCKED
             LIMIT 20
            """, nativeQuery = true)
    List<OutboxEntity> buscarPendentes(
            @Param("agora") Instant agora,
            @Param("limiteRecuperacao") Instant limiteRecuperacao);
}
