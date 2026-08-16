package com.eventops.organization.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, UUID> {
    Optional<OrganizationMemberEntity> findByOrganizacaoIdAndEmailNormalizadoAndAtivoTrue(
            UUID organizacaoId, String emailNormalizado);
    boolean existsByOrganizacaoIdAndEmailNormalizado(UUID organizacaoId, String emailNormalizado);
    List<OrganizationMemberEntity> findByEmailNormalizadoAndAtivoTrueOrderByCriadoEmDesc(String emailNormalizado);
}
