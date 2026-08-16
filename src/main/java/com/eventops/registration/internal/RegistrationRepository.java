package com.eventops.registration.internal;

import com.eventops.registration.RegistrationStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {
    boolean existsByEventoIdAndEmailNormalizado(UUID eventoId, String emailNormalizado);
    boolean existsByCodigoIndicacao(String codigoIndicacao);
    Optional<RegistrationEntity> findByEventoIdAndCodigoIndicacaoAndStatus(
            UUID eventoId, String codigoIndicacao, RegistrationStatus status);
    Page<RegistrationEntity> findByEventoIdOrderByCriadoEmDesc(UUID eventoId, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inscricao from RegistrationEntity inscricao where inscricao.id = :id")
    Optional<RegistrationEntity> buscarParaAtualizar(@Param("id") UUID id);
}
