# Banco de dados

## Conexao local

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `54324` |
| Banco | `eventops` |
| Usuario | `eventops` |
| Senha | `eventops` |

## Tabelas

| Tabela | Finalidade |
|---|---|
| `organizacoes` | tenant proprietario dos eventos |
| `membros_organizacao` | papeis de acesso por email verificado |
| `eventos` | agenda, capacidade, ocupacao e estado |
| `inscricoes` | participante, origem e estado da vaga |
| `lista_espera` | ordem FIFO e historico de promocao |
| `convites` | convites individuais com expiracao |
| `credenciais` | hash do token e ciclo da credencial |
| `check_ins` | entrada unica por inscricao |
| `requisicoes_idempotentes` | resposta persistida por chave logica |
| `notificacoes` | entrega, tentativas e erros SMTP |
| `eventos_outbox` | fila transacional retomavel |
| `auditorias` | trilha de alteracoes e correlacao |

## Consultas uteis

```sql
SELECT titulo, capacidade, vagas_ocupadas, status FROM eventos ORDER BY criado_em DESC;

SELECT nome, email, status, origem FROM inscricoes ORDER BY criado_em;

SELECT status, COUNT(*) FROM notificacoes GROUP BY status;

SELECT ator, acao, recurso, recurso_id, criado_em
FROM auditorias
ORDER BY criado_em DESC
LIMIT 30;
```

O schema e administrado exclusivamente pelo Flyway. A aplicacao usa `ddl-auto=validate`, portanto Hibernate nunca altera tabelas silenciosamente.
