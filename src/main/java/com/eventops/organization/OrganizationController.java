package com.eventops.organization;

import com.eventops.audit.AuditResponse;
import com.eventops.audit.AuditService;
import com.eventops.shared.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.EnumSet;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizacoes")
@SecurityRequirement(name = "oauth2")
public class OrganizationController {

    private final OrganizationService servico;
    private final AuditService auditoria;

    public OrganizationController(OrganizationService servico, AuditService auditoria) {
        this.servico = servico;
        this.auditoria = auditoria;
    }

    @PostMapping
    ResponseEntity<OrganizationResponse> criar(@Valid @RequestBody CreateOrganizationRequest requisicao) {
        OrganizationResponse resposta = servico.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/v1/organizacoes/" + resposta.id())).body(resposta);
    }

    @GetMapping
    List<OrganizationResponse> listarMinhas() {
        return servico.listarMinhas();
    }

    @PostMapping("/{organizacaoId}/membros")
    ResponseEntity<MemberResponse> adicionarMembro(
            @PathVariable UUID organizacaoId,
            @Valid @RequestBody AddMemberRequest requisicao) {
        MemberResponse resposta = servico.adicionarMembro(organizacaoId, requisicao);
        return ResponseEntity.created(URI.create("/api/v1/organizacoes/" + organizacaoId + "/membros/" + resposta.id()))
                .body(resposta);
    }

    @GetMapping("/{organizacaoId}/auditorias")
    PageResponse<AuditResponse> listarAuditorias(@PathVariable UUID organizacaoId, Pageable pagina) {
        servico.exigirAcesso(organizacaoId, EnumSet.of(OrganizationRole.GESTOR_EVENTO));
        return PageResponse.de(auditoria.listar(organizacaoId, pagina));
    }
}
