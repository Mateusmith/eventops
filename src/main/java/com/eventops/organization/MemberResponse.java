package com.eventops.organization;

import java.util.UUID;

public record MemberResponse(UUID id, String nome, String email, OrganizationRole papel, boolean ativo) {
}
