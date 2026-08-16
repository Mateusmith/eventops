package com.eventops.registration.internal;

import com.eventops.registration.WaitingListStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitingListRepository extends JpaRepository<WaitingListEntity, UUID> {
    Optional<WaitingListEntity> findByInscricaoId(UUID inscricaoId);
    long countByEventoIdAndStatus(UUID eventoId, WaitingListStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from WaitingListEntity item
             where item.eventoId = :eventoId
               and item.status = com.eventops.registration.WaitingListStatus.AGUARDANDO
             order by item.entrouEm
            """)
    List<WaitingListEntity> buscarPrimeirosParaPromocao(@Param("eventoId") UUID eventoId, Pageable pagina);
}
