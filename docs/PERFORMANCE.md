# Desempenho e teste de carga

## Objetivo

O benchmark verifica o ponto mais sensivel do dominio: muitas pessoas disputando poucas vagas ao mesmo tempo. O teste mede desempenho e tambem consulta o PostgreSQL para provar as invariantes de negocio.

## Cenarios

1. **Abertura de inscricoes:** 2.000 usuarios virtuais enviam uma inscricao cada, simultaneamente, para um evento com 100 vagas.
2. **Cancelamento e promocao:** 50 inscricoes confirmadas sao canceladas em paralelo e devem promover exatamente as 50 primeiras pessoas da fila.

As notificacoes continuam sendo persistidas na outbox, mas o entregador SMTP fica pausado durante a medicao para nao misturar o custo de um servico externo com a latencia da API.

## Invariantes obrigatorias

- 2.000 emails distintos produzem 2.000 inscricoes, sem respostas HTTP inesperadas.
- Exatamente 100 inscricoes ficam confirmadas; nunca pode haver overbooking.
- Antes dos cancelamentos, 1.900 inscricoes ficam na lista de espera.
- Os 50 cancelamentos deixam 50 inscricoes canceladas e promovem 50 itens distintos.
- Depois das promocoes, o evento continua com 100 vagas ocupadas e 1.850 pessoas aguardando.
- O conjunto promovido corresponde aos 50 primeiros itens da fila ordenada por `entrou_em, id`.

Qualquer divergencia encerra o script com codigo de erro.

## Orcamento

| Indicador | Inscricoes | Cancelamentos |
|---|---:|---:|
| Erros HTTP | 0% | 0% |
| p95 | menor que 120 s | menor que 8 s |
| p99 | menor que 180 s | menor que 10 s |
| Regra de negocio | 100 confirmadas | 50 promocoes FIFO |

A abertura representa uma rajada extrema e totalmente sincronizada, nao uma rampa de trafego. O tempo inclui a fila de backpressure do pool JDBC; o sistema prefere aguardar de forma limitada a perder inscricoes ou ultrapassar a capacidade.

## Resultado de referencia

Execucao local em 17 de agosto de 2026:

| Indicador | Inscricoes | Cancelamentos |
|---|---:|---:|
| Requisicoes | 2.000 | 50 |
| Throughput | 58,46 req/s | 12,06 req/s |
| Erros | 0% | 0% |
| p50 | 21,62 s | 2,62 s |
| p95 | 30,01 s | 4,02 s |
| p99 | 30,32 s | 4,09 s |
| Maximo | 30,65 s | 4,11 s |
| CPU maxima da aplicacao | 361,28% | 325,50% |
| Memoria maxima da aplicacao | 870,00 MiB | 861,50 MiB |
| CPU maxima do PostgreSQL | 85,66% | 44,94% |
| Conexoes ativas observadas | 15 | 14 |
| Locks nao concedidos observados | 14 | 14 |

Resultado funcional: 100 confirmadas, 1.850 em espera, 50 canceladas, 50 promovidas, capacidade final de 100 e FIFO valido.

## Ambiente de referencia

| Item | Configuracao |
|---|---|
| Processador | Intel Core i7-7500U, 2 nucleos e 4 processadores logicos |
| Memoria fisica | 15,9 GB |
| Sistema | Windows 10 22H2 com Docker Desktop |
| Docker Engine | 29.3.1 |
| Pool HikariCP | 15 conexoes |
| Timeout para obter conexao | 120 s |
| Gerador de carga | k6 1.1.0 em container |

CPU acima de 100% representa uso simultaneo de mais de um processador logico, conforme a metrica do Docker.

## Executar

Com a plataforma saudavel:

```powershell
.\scripts\run-load-test.ps1
```

Para um ensaio menor durante o desenvolvimento:

```powershell
.\scripts\run-load-test.ps1 -Tentativas 500 -Capacidade 25 -Cancelamentos 10
```

Os arquivos brutos sao gerados em `performance/results`: resumos do k6, logs, amostras de recursos e `load-test-result.json`. A pasta e ignorada pelo Git para evitar publicar ruido especifico da maquina.

O workflow `benchmark-manual` permite repetir o teste sob demanda no GitHub Actions. Ele nao roda em toda pull request porque e um teste caro e sensivel ao hardware compartilhado do runner.

## Leitura arquitetural

A atualizacao condicional de `eventos.vagas_ocupadas` concentra disputa em uma unica linha, o que aparece nos locks observados. Essa escolha fornece consistencia forte e impede overbooking sem depender de Redis ou de reconciliacao eventual. Para uma escala muito maior, a evolucao natural seria separar a admissao em lotes ou usar reservas de vaga particionadas, mantendo uma rotina de reconciliacao como rede de seguranca.
