package com.eventops.event.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    boolean existsBySlug(String slug);

    Optional<EventEntity> findBySlug(String slug);

    Page<EventEntity> findByOrganizacaoIdOrderByCriadoEmDesc(UUID organizacaoId, Pageable pagina);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE eventos
               SET vagas_ocupadas = vagas_ocupadas + 1,
                   atualizado_em = CURRENT_TIMESTAMP,
                   versao = versao + 1
             WHERE id = :eventoId
               AND status = 'PUBLICADO'
               AND inicio_em > CURRENT_TIMESTAMP
               AND (capacidade IS NULL OR vagas_ocupadas < capacidade)
            """, nativeQuery = true)
    int reservarVaga(@Param("eventoId") UUID eventoId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE eventos
               SET vagas_ocupadas = GREATEST(vagas_ocupadas - 1, 0),
                   atualizado_em = CURRENT_TIMESTAMP,
                   versao = versao + 1
             WHERE id = :eventoId
            """, nativeQuery = true)
    int liberarVaga(@Param("eventoId") UUID eventoId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM eventos
                 WHERE id = :eventoId
                   AND status = 'PUBLICADO'
                   AND inicio_em > CURRENT_TIMESTAMP
            )
            """, nativeQuery = true)
    boolean aceitaInscricoes(@Param("eventoId") UUID eventoId);
}
