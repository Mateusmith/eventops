package com.eventops.configuration;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionSecurityValidator {

    private static final Set<String> SENHAS_DEMONSTRATIVAS = Set.of(
            "admin", "eventops", "eventops123", "password", "prometheus@123",
            "substitua-localmente", "troque-esta-senha-local");

    public ProductionSecurityValidator(Environment ambiente, ObservabilityProperties observabilidade) {
        validarSenha("spring.datasource.password", ambiente.getProperty("spring.datasource.password"));
        validarSenha("spring.mail.password", ambiente.getProperty("spring.mail.password"));
        validarSenha("eventops.observabilidade.senha", observabilidade.senha());
        validarHttps("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                ambiente.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
        validarHttps("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                ambiente.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri"));
        validarHttps("eventops.url-publica", ambiente.getProperty("eventops.url-publica"));
        validarPortaGerenciamento(ambiente);
    }

    private void validarSenha(String propriedade, String senha) {
        if (senha == null || senha.length() < 16
                || SENHAS_DEMONSTRATIVAS.contains(senha.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    propriedade + " deve vir de um segredo externo e possuir pelo menos 16 caracteres.");
        }
    }

    private void validarHttps(String propriedade, String valor) {
        String esquema = null;
        try {
            esquema = valor == null ? null : URI.create(valor).getScheme();
        } catch (IllegalArgumentException ignorada) {
            // A mensagem de validacao abaixo mantem a configuracao operacional acionavel.
        }
        if (!"https".equalsIgnoreCase(esquema)) {
            throw new IllegalStateException(propriedade + " deve usar HTTPS no perfil production.");
        }
    }

    private void validarPortaGerenciamento(Environment ambiente) {
        int portaAplicacao = ambiente.getProperty("server.port", Integer.class, 8080);
        int portaGerenciamento = ambiente.getProperty("management.server.port", Integer.class, portaAplicacao);
        if (portaAplicacao == portaGerenciamento) {
            throw new IllegalStateException(
                    "management.server.port deve ser diferente de server.port no perfil production.");
        }
    }
}
