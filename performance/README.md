# Testes de desempenho

O benchmark usa k6 em um container efemero e mede dois cenarios independentes:

1. `registrations.js`: 2.000 usuarios virtuais fazem uma inscricao simultanea em um evento de 100 vagas.
2. `cancellations.js`: 50 cancelamentos concorrentes liberam vagas e promovem a fila.

O orquestrador valida diretamente no PostgreSQL que nao houve excesso de capacidade, duplicidade ou quebra da ordem FIFO. Durante cada cenario, `monitor.ps1` amostra CPU, memoria, conexoes ativas e locks observados.

```powershell
.\scripts\run-load-test.ps1
```

Resultados brutos sao gravados em `performance/results` e ignorados pelo Git. O orcamento e o ultimo resultado de referencia ficam documentados em `docs/PERFORMANCE.md`.
