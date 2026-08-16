package com.eventops.credential;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialController {

    private final CredentialService servico;
    private final QrCodeService qrCode;

    public CredentialController(CredentialService servico, QrCodeService qrCode) {
        this.servico = servico;
        this.qrCode = qrCode;
    }

    @GetMapping("/api/v1/publico/credenciais/{token}")
    PublicCredentialResponse obter(@PathVariable String token) {
        return servico.obterPublica(token);
    }

    @GetMapping(value = "/api/v1/publico/credenciais/{token}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> gerarQrCode(@PathVariable String token) {
        servico.obterPublica(token);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode.gerar(token));
    }

    @PostMapping("/api/v1/credenciais/inscricoes/{inscricaoId}/renovacao")
    @SecurityRequirement(name = "oauth2")
    IssuedCredential renovar(@PathVariable UUID inscricaoId) {
        return servico.renovar(inscricaoId);
    }
}
