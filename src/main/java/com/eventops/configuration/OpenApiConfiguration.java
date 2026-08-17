package com.eventops.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI gestaoEventosOpenApi(
            @Value("${EVENTOPS_URL_AUTORIZACAO:http://localhost:18082/realms/eventops/protocol/openid-connect/auth}")
            String urlAutorizacao,
            @Value("${EVENTOPS_URL_TOKEN:http://localhost:18082/realms/eventops/protocol/openid-connect/token}")
            String urlToken) {
        var fluxo = new OAuthFlow().authorizationUrl(urlAutorizacao).tokenUrl(urlToken);
        var esquema = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(fluxo));

        return new OpenAPI()
                .info(new Info()
                        .title("Gestão de Eventos API")
                        .version("v1")
                        .description("Operacao segura de eventos, inscricoes, indicacoes e check-in."))
                .components(new Components().addSecuritySchemes("oauth2", esquema));
    }
}
