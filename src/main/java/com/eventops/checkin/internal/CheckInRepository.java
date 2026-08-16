package com.eventops.checkin.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckInEntity, UUID> {
    Optional<CheckInEntity> findByInscricaoId(UUID inscricaoId);
    Page<CheckInEntity> findByEventoIdOrderByRealizadoEmDesc(UUID eventoId, Pageable pagina);
}
