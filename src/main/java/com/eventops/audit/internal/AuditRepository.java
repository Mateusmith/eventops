package com.eventops.audit.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntity, UUID> {
    Page<AuditEntity> findByOrganizacaoIdOrderByCriadoEmDesc(UUID organizacaoId, Pageable pagina);
}
