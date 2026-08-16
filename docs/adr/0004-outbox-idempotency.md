# ADR 0004: Outbox e idempotencia

Status: aceito.

## Contexto

SMTP pode falhar depois do commit e leitores de QR podem repetir chamadas por timeout ou duplo toque.

## Decisao

Persistir notificacao e outbox na transacao de negocio. Processar lotes com retry e `SKIP LOCKED`. No check-in, persistir resposta por `ator + operacao + Idempotency-Key`.

## Consequencias

Falhas externas nao desfazem a regra principal e chamadas repetidas sao seguras. Tabelas operacionais precisam de limpeza e monitoramento em producao.
