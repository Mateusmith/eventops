package com.eventops.checkin;

import com.eventops.shared.ConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final JdbcClient jdbc;
    private final ObjectMapper mapeador;
    private final Clock relogio;

    public IdempotencyService(JdbcClient jdbc, ObjectMapper mapeador, Clock relogio) {
        this.jdbc = jdbc;
        this.mapeador = mapeador;
        this.relogio = relogio;
    }

    public IdempotencyDecision iniciar(String ator, String operacao, String chave, String hashRequisicao) {
        if (chave == null || !chave.matches("[A-Za-z0-9._:-]{8,120}")) {
            throw new ConflictException("CHAVE_IDEMPOTENCIA_INVALIDA",
                    "Idempotency-Key deve ter entre 8 e 120 caracteres seguros.");
        }
        UUID id = UUID.randomUUID();
        Instant agora = Instant.now(relogio);
        int inseridos = jdbc.sql("""
                INSERT INTO requisicoes_idempotentes
                    (id, ator, operacao, chave, hash_requisicao, codigo_resposta, corpo_resposta, criado_em, expira_em)
                VALUES
                    (:id, :ator, :operacao, :chave, :hash, 0, '{}'::jsonb, :agora, :expiraEm)
                ON CONFLICT (ator, operacao, chave) DO NOTHING
                """)
                .params(Map.of(
                        "id", id,
                        "ator", ator,
                        "operacao", operacao,
                        "chave", chave,
                        "hash", hashRequisicao,
                        "agora", Timestamp.from(agora),
                        "expiraEm", Timestamp.from(agora.plus(Duration.ofHours(24)))))
                .update();
        if (inseridos == 1) {
            return new IdempotencyDecision(id, true, 0, mapeador.createObjectNode());
        }

        return jdbc.sql("""
                SELECT id, hash_requisicao, codigo_resposta, corpo_resposta::text AS corpo
                  FROM requisicoes_idempotentes
                 WHERE ator = :ator AND operacao = :operacao AND chave = :chave
                """)
                .params(Map.of("ator", ator, "operacao", operacao, "chave", chave))
                .query((resultado, numeroLinha) -> {
                    String hashExistente = resultado.getString("hash_requisicao");
                    if (!hashExistente.equals(hashRequisicao)) {
                        throw new ConflictException("CHAVE_IDEMPOTENCIA_REUTILIZADA",
                                "A chave ja foi usada com outro conteudo.");
                    }
                    try {
                        return new IdempotencyDecision(
                                resultado.getObject("id", UUID.class),
                                false,
                                resultado.getInt("codigo_resposta"),
                                mapeador.readTree(resultado.getString("corpo")));
                    } catch (JsonProcessingException excecao) {
                        throw new IllegalStateException("Resposta idempotente invalida.", excecao);
                    }
                })
                .single();
    }

    public void concluir(UUID id, int codigoResposta, JsonNode corpoResposta) {
        jdbc.sql("""
                UPDATE requisicoes_idempotentes
                   SET codigo_resposta = :codigo,
                       corpo_resposta = CAST(:corpo AS jsonb)
                 WHERE id = :id
                """)
                .param("codigo", codigoResposta)
                .param("corpo", corpoResposta.toString())
                .param("id", id)
                .update();
    }
}
