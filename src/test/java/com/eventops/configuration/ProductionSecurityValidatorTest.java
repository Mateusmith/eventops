package com.eventops.configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityValidatorTest {

    private static final ObservabilityProperties OBSERVABILIDADE_SEGURA =
            new ObservabilityProperties("prometheus", "Observabilidade@2026-Segura");

    @Test
    void deveAceitarConfiguracaoOperacionalSegura() {
        assertThatCode(() -> new ProductionSecurityValidator(ambienteSeguro(), OBSERVABILIDADE_SEGURA))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRecusarSenhaDemonstrativaDoBanco() {
        MockEnvironment ambiente = ambienteSeguro()
                .withProperty("spring.datasource.password", "eventops");

        assertThatThrownBy(() -> new ProductionSecurityValidator(ambiente, OBSERVABILIDADE_SEGURA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password");
    }

    @Test
    void deveRecusarOidcSemHttps() {
        MockEnvironment ambiente = ambienteSeguro()
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://identidade.local/realms/eventops");

        assertThatThrownBy(() -> new ProductionSecurityValidator(ambiente, OBSERVABILIDADE_SEGURA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deve usar HTTPS");
    }

    @Test
    void deveRecusarMarcadorDemonstrativoDaObservabilidade() {
        var observabilidadeDemonstrativa =
                new ObservabilityProperties("prometheus", "troque-esta-senha-local");

        assertThatThrownBy(() -> new ProductionSecurityValidator(ambienteSeguro(), observabilidadeDemonstrativa))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eventops.observabilidade.senha");
    }

    @Test
    void deveRecusarActuatorNaMesmaPortaPublicaDaApi() {
        MockEnvironment ambiente = ambienteSeguro()
                .withProperty("management.server.port", "8080");

        assertThatThrownBy(() -> new ProductionSecurityValidator(ambiente, OBSERVABILIDADE_SEGURA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("management.server.port");
    }

    private MockEnvironment ambienteSeguro() {
        return new MockEnvironment()
                .withProperty("spring.datasource.password", "Banco@2026-SenhaMuitoSegura")
                .withProperty("spring.mail.password", "Email@2026-SenhaMuitoSegura")
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://identidade.example.com/realms/eventops")
                .withProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "https://identidade.example.com/realms/eventops/protocol/openid-connect/certs")
                .withProperty("eventops.url-publica", "https://eventos.example.com")
                .withProperty("server.port", "8080")
                .withProperty("management.server.port", "9090");
    }
}
