package com.eventops.organization.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    boolean existsBySlug(String slug);
    Optional<OrganizationEntity> findByIdAndAtivaTrue(UUID id);
}
