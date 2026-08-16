# Postman

Importe `EventOps.postman_collection.json` e execute as requisicoes na ordem exibida ou use o Collection Runner.

A colecao possui apenas variaveis locais e captura automaticamente tokens JWT, IDs, slug, convite, indicacao, credencial, chave idempotente e token de cancelamento. Nenhum environment separado e necessario.

Antes de executar:

```powershell
docker compose up -d --build
docker compose ps
```

Espere `aplicacao`, `banco` e `mailpit` ficarem saudaveis e o Keycloak responder em `http://localhost:18082`.
