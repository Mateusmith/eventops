package com.eventops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventops.registration.CreateRegistrationRequest;
import com.eventops.registration.RegistrationResponse;
import com.eventops.registration.RegistrationService;
import com.eventops.registration.RegistrationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureObservability
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventOpsEndToEndIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("eventops_testes")
            .withUsername("eventops")
            .withPassword("eventops");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapeador;

    @Autowired
    RegistrationService inscricoes;

    @Test
    void deveExecutarFluxoCompletoComIndicacaoConviteFilaECheckInIdempotente() throws Exception {
        UUID organizacaoId = criarOrganizacao("Equipe Summit " + UUID.randomUUID());
        JsonNode evento = criarEPublicarEvento(organizacaoId, "Summit " + UUID.randomUUID(), 3);
        UUID eventoId = UUID.fromString(evento.get("id").asText());
        String slug = evento.get("slug").asText();

        JsonNode ana = inscrever(slug, "Ana Souza", "ana." + UUID.randomUUID() + "@example.com", null, null);
        assertThat(ana.get("status").asText()).isEqualTo("CONFIRMADA");

        JsonNode bruno = inscrever(slug, "Bruno Lima", "bruno." + UUID.randomUUID() + "@example.com",
                null, ana.get("codigoIndicacao").asText());
        mvc.perform(get("/api/v1/publico/eventos/{slug}/ranking", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participante").value("Ana S."))
                .andExpect(jsonPath("$[0].indicacoesConfirmadas").value(1));

        String emailCarla = "carla." + UUID.randomUUID() + "@example.com";
        JsonNode convite = json(mvc.perform(post("/api/v1/eventos/{id}/convites", eventoId)
                        .with(organizador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeador.writeValueAsBytes(Map.of(
                                "nome", "Carla Dias",
                                "email", emailCarla,
                                "expiraEm", Instant.now().plus(1, ChronoUnit.DAYS)))))
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode carla = inscrever(slug, "Carla Dias", emailCarla, convite.get("token").asText(), null);
        assertThat(carla.get("origem").asText()).isEqualTo("CONVITE");

        JsonNode diego = inscrever(slug, "Diego Reis", "diego." + UUID.randomUUID() + "@example.com", null, null);
        assertThat(diego.get("status").asText()).isEqualTo("LISTA_ESPERA");
        assertThat(diego.get("posicaoListaEspera").asLong()).isEqualTo(1);

        String tokenCredencialAna = ana.at("/credencial/token").asText();
        String chave = "check-in-" + UUID.randomUUID();
        byte[] corpoCheckIn = mapeador.writeValueAsBytes(Map.of("tokenCredencial", tokenCredencialAna));
        mvc.perform(post("/api/v1/eventos/{id}/check-ins", eventoId)
                        .with(organizador())
                        .header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCheckIn))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/eventos/{id}/check-ins", eventoId)
                        .with(organizador())
                        .header("Idempotency-Key", chave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCheckIn))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"));

        UUID brunoId = UUID.fromString(bruno.get("id").asText());
        mvc.perform(post("/api/v1/publico/inscricoes/{id}/cancelamento", brunoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeador.writeValueAsBytes(Map.of(
                                "tokenCancelamento", bruno.get("tokenCancelamento").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inscricaoPromovidaId").value(diego.get("id").asText()));

        mvc.perform(get("/api/v1/publico/eventos/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagasOcupadas").value(3))
                .andExpect(jsonPath("$.vagasDisponiveis").value(0));

        mvc.perform(get("/api/v1/eventos/{id}/inscricoes", eventoId).with(organizador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4));
    }

    @Test
    void deveImpedirExcessoDeCapacidadeSobConcorrencia() throws Exception {
        UUID organizacaoId = criarOrganizacao("Concorrencia " + UUID.randomUUID());
        JsonNode evento = criarEPublicarEvento(organizacaoId, "Ultima Vaga " + UUID.randomUUID(), 1);
        String slug = evento.get("slug").asText();

        List<Callable<RegistrationResponse>> tarefas = new ArrayList<>();
        for (int indice = 0; indice < 8; indice++) {
            int numero = indice;
            tarefas.add(() -> inscricoes.inscrever(slug, new CreateRegistrationRequest(
                    "Pessoa " + numero, "pessoa." + numero + "." + UUID.randomUUID() + "@example.com", null, null)));
        }

        List<RegistrationResponse> respostas;
        try (var executor = Executors.newFixedThreadPool(8)) {
            respostas = executor.invokeAll(tarefas).stream().map(futuro -> {
                try {
                    return futuro.get();
                } catch (Exception excecao) {
                    throw new AssertionError(excecao);
                }
            }).toList();
        }

        assertThat(respostas).filteredOn(item -> item.status() == RegistrationStatus.CONFIRMADA).hasSize(1);
        assertThat(respostas).filteredOn(item -> item.status() == RegistrationStatus.LISTA_ESPERA).hasSize(7);
        mvc.perform(get("/api/v1/publico/eventos/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vagasOcupadas").value(1));
    }

    @Test
    void deveProtegerRotasAdministrativas() throws Exception {
        mvc.perform(post("/api/v1/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem token\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/actuator/prometheus").with(user("operador").roles("OPERACAO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/actuator/prometheus")
                        .with(httpBasic("prometheus", "TesteObservabilidade@123")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/publico/recurso-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void deveIsolarDadosEntreOrganizacoes() throws Exception {
        UUID organizacaoAna = criarOrganizacao(
                "Organizacao Ana " + UUID.randomUUID(),
                usuario("usuario-ana", "ana.gestora@eventops.local", "Ana Gestora", "ana"));
        UUID organizacaoBruno = criarOrganizacao(
                "Organizacao Bruno " + UUID.randomUUID(),
                usuario("usuario-bruno", "bruno.gestor@eventops.local", "Bruno Gestor", "bruno"));

        mvc.perform(get("/api/v1/eventos/organizacao/{organizacaoId}", organizacaoAna)
                        .with(usuario("usuario-ana", "ana.gestora@eventops.local", "Ana Gestora", "ana")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/eventos/organizacao/{organizacaoId}", organizacaoBruno)
                        .with(usuario("usuario-ana", "ana.gestora@eventops.local", "Ana Gestora", "ana")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_ACESSO_ORGANIZACAO"));

        mvc.perform(get("/api/v1/organizacoes/{organizacaoId}/auditorias", organizacaoBruno)
                        .with(usuario("usuario-ana", "ana.gestora@eventops.local", "Ana Gestora", "ana")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_ACESSO_ORGANIZACAO"));
    }

    private UUID criarOrganizacao(String nome) throws Exception {
        return criarOrganizacao(nome, organizador());
    }

    private UUID criarOrganizacao(String nome, JwtRequestPostProcessor autenticacao) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/organizacoes")
                        .with(autenticacao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeador.writeValueAsBytes(Map.of("nome", nome))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(resultado).get("id").asText());
    }

    private JsonNode criarEPublicarEvento(UUID organizacaoId, String titulo, int capacidade) throws Exception {
        Instant inicio = Instant.now().plus(2, ChronoUnit.DAYS);
        JsonNode criado = json(mvc.perform(post("/api/v1/eventos")
                        .with(organizador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeador.writeValueAsBytes(Map.of(
                                "organizacaoId", organizacaoId,
                                "titulo", titulo,
                                "descricao", "Evento profissional para testes",
                                "local", "Centro de Convencoes",
                                "fusoHorario", "America/Sao_Paulo",
                                "inicioEm", inicio,
                                "fimEm", inicio.plus(8, ChronoUnit.HOURS),
                                "capacidade", capacidade))))
                .andExpect(status().isCreated())
                .andReturn());
        return json(mvc.perform(post("/api/v1/eventos/{id}/publicacao", criado.get("id").asText())
                        .with(organizador()))
                .andExpect(status().isOk())
                .andReturn());
    }

    private JsonNode inscrever(String slug, String nome, String email, String convite, String indicacao) throws Exception {
        var corpo = mapeador.createObjectNode();
        corpo.put("nome", nome);
        corpo.put("email", email);
        if (convite != null) corpo.put("tokenConvite", convite);
        if (indicacao != null) corpo.put("codigoIndicacao", indicacao);
        return json(mvc.perform(post("/api/v1/publico/eventos/{slug}/inscricoes", slug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapeador.writeValueAsBytes(corpo)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return mapeador.readTree(resultado.getResponse().getContentAsByteArray());
    }

    private JwtRequestPostProcessor organizador() {
        return usuario(
                "usuario-organizador",
                "organizador@eventops.local",
                "Olivia Organizadora",
                "organizador");
    }

    private JwtRequestPostProcessor usuario(String id, String email, String nome, String nomeUsuario) {
        return jwt().jwt(token -> token
                .subject(id)
                .claim("email", email)
                .claim("name", nome)
                .claim("preferred_username", nomeUsuario));
    }
}
