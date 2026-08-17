# Gestão de Eventos

API Java para operacao segura de eventos presenciais: organizacoes e equipes, ciclo de vida do evento, capacidade concorrente, inscricoes, lista de espera, convites, indicacoes, credenciais QR, check-in idempotente, notificacoes e auditoria.

O projeto foi construido do zero como uma peca de portfolio. A proposta e resolver problemas que aparecem quando um sistema didatico de eventos encontra trafego real: duas pessoas disputando a ultima vaga, leitura repetida do mesmo QR Code, controle de acesso por organizacao, retomada de notificacoes e rastreabilidade das decisoes.

## Destaques tecnicos

- Java 21, Spring Boot 3.5, Spring Modulith e Maven Wrapper.
- PostgreSQL 16 com seis migrations Flyway e `ddl-auto=validate`.
- OAuth2/OIDC com Keycloak e associacao de papeis por organizacao.
- Reserva atomica de capacidade, restricoes unicas e transacoes.
- Lista de espera FIFO com promocao automatica no cancelamento.
- Tokens opacos de 256 bits; apenas SHA-256 e persistido para credenciais, convites e cancelamentos.
- Check-in com `Idempotency-Key` e resposta reproduzida com seguranca.
- Outbox transacional, retry exponencial, `SKIP LOCKED` e recuperacao de trabalho abandonado.
- Auditoria com usuario, recurso, dados e `X-Correlation-Id`.
- OpenAPI, Postman, Mailpit, Actuator, Prometheus e Grafana.
- Testes unitarios, arquitetura modular, integracao e concorrencia com Testcontainers.

## Inicio rapido

Requisitos: Docker Desktop. Java local so e necessario para executar os testes fora do container.

```powershell
git clone https://github.com/Mateusmith/gestao-eventos.git
cd gestao-eventos
.\scripts\setup-local.ps1
docker compose --profile observability up -d --build
docker compose ps
```

O script cria `.env` e o arquivo local usado no scrape sem sobrescrever configuracoes existentes. Troque os marcadores demonstrativos se o ambiente puder ser acessado por outra pessoa. Na primeira inicializacao, Keycloak e Maven podem levar cerca de um minuto para preparar os caches.

| Recurso | Endereco |
|---|---|
| API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Keycloak | http://localhost:18082 |
| Mailpit | http://localhost:18025 |
| Prometheus | http://localhost:19091 |
| Grafana | http://localhost:13001 |
| PostgreSQL | `localhost:54324` |

## Usuarios locais

| Papel | Usuario | Senha | Email |
|---|---|---|---|
| Organizador | `organizador` | `eventops123` | `organizador@eventops.local` |
| Operador | `operador` | `eventops123` | `operador@eventops.local` |

Essas credenciais existem somente no ambiente demonstrativo. O cliente publico `eventops-postman` nao possui segredo e o password grant deve permanecer restrito ao laboratorio.
No Grafana, use os valores `EVENTOPS_GRAFANA_ADMIN_USER` e `EVENTOPS_GRAFANA_ADMIN_PASSWORD` do seu `.env`; o painel `Gestão de Eventos - Operação` e provisionado automaticamente.

O nome publico do produto e **Gestão de Eventos**. Os prefixos tecnicos `EVENTOPS_*`, o pacote `com.eventops` e o realm `eventops` foram preservados para manter compatibilidade com ambientes existentes.

## Teste real automatizado

Com os containers saudaveis:

```powershell
.\scripts\test-api.ps1
.\scripts\database-summary.ps1
```

O primeiro script obtem tokens reais no Keycloak, cria organizacao e evento, adiciona um operador, disputa capacidade, realiza check-in duas vezes com a mesma chave, cancela uma inscricao, promove a fila e consulta auditoria.

A mesma jornada pode ser executada pela collection, na interface do Postman ou pelo Newman:

```powershell
npx --yes newman run postman/GestaoEventos.postman_collection.json
```

## Teste de carga

O benchmark reproduz uma abertura com 2.000 inscricoes concorrentes para 100 vagas e, em seguida, 50 cancelamentos concorrentes com promocao FIFO:

```powershell
.\scripts\run-load-test.ps1
```

O teste falha se houver overbooking, duplicidade, erro HTTP ou quebra da fila. Metodologia, orcamento e resultado de referencia estao em [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

## Observabilidade

O perfil `observability` inicia Prometheus e Grafana. O dashboard versionado acompanha check-ins, repeticoes idempotentes, requisicoes HTTP, latencia p95 e heap da JVM. O Actuator usa a porta interna `9090`, que nao e publicada no host: `health` e publico apenas nessa rede e as metricas exigem a credencial Basic compartilhada por Docker secret com o Prometheus.

## Testes Java

```powershell
# Garanta que JAVA_HOME aponte para uma instalacao do JDK 21.
.\mvnw.cmd verify
```

`verify` executa a verificacao do Spring Modulith e usa um PostgreSQL 16 descartavel via Testcontainers. Docker deve estar ativo.

## Banco de dados

```text
host: localhost
porta: 54324
banco: valor de EVENTOPS_POSTGRES_DB
usuario: valor de EVENTOPS_POSTGRES_USER
senha: valor de EVENTOPS_POSTGRES_PASSWORD
```

Exemplo de acesso:

```powershell
docker compose exec banco sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

O Compose carrega `.env` automaticamente; o comando usa as variaveis ja presentes no container.

As tabelas e colunas estao em portugues. Tokens secretos nunca sao armazenados em texto puro.

## Fluxo principal

1. Autentique o organizador no Keycloak.
2. Crie uma organizacao e associe membros.
3. Crie um evento em `RASCUNHO` e publique-o.
4. Inscreva participantes pela rota publica.
5. Leia o QR Code e envie o token ao endpoint protegido de check-in.
6. Use sempre uma `Idempotency-Key` nova por tentativa logica.

A colecao em [`postman/GestaoEventos.postman_collection.json`](postman/GestaoEventos.postman_collection.json) captura automaticamente IDs, slug, tokens e codigos entre as requisicoes. Os corpos completos tambem estao em [`docs/API_EXAMPLES.md`](docs/API_EXAMPLES.md).

## Documentacao

- [Arquitetura e modulos](docs/ARCHITECTURE.md)
- [Exemplos da API](docs/API_EXAMPLES.md)
- [Banco de dados](docs/DATABASE.md)
- [Desempenho e teste de carga](docs/PERFORMANCE.md)
- [Preparacao para producao](docs/PRODUCTION.md)
- [Decisoes arquiteturais](docs/adr/README.md)
- [Politica de seguranca](SECURITY.md)
- [Como contribuir](CONTRIBUTING.md)

## Encerramento

```powershell
docker compose down
```

Para apagar tambem os dados locais, execute conscientemente `docker compose down -v`.

## Licenca

Distribuido sob a licenca MIT. Consulte [LICENSE](LICENSE).
