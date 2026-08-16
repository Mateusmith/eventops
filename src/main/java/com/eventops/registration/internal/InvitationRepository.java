package com.eventops.registration.internal;

import com.eventops.registration.InvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    boolean existsByEventoIdAndEmailNormalizadoAndStatus(
            UUID eventoId, String emailNormalizado, InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvitationEntity> findByTokenHash(String tokenHash);
}
