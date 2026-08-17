# Exemplos da API

Substitua valores entre chaves. A colecao Postman faz essa captura automaticamente.

## Autenticacao local

```http
POST http://localhost:18082/realms/eventops/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=eventops-postman&username=organizador&password=eventops123
```

Use `Authorization: Bearer {access_token}` nas rotas administrativas.

## Organizacao

```http
POST /api/v1/organizacoes
Authorization: Bearer {token}
Content-Type: application/json

{
  "nome": "Comunidade Java Brasil",
  "documento": "12.345.678/0001-90"
}
```

```http
POST /api/v1/organizacoes/{organizacaoId}/membros
Authorization: Bearer {token}
Content-Type: application/json

{
  "nome": "Carlos Check-in",
  "email": "operador@eventops.local",
  "papel": "OPERADOR_CHECKIN"
}
```

Papeis aceitos: `PROPRIETARIO`, `GESTOR_EVENTO` e `OPERADOR_CHECKIN`.

## Evento

```http
POST /api/v1/eventos
Authorization: Bearer {token}
Content-Type: application/json

{
  "organizacaoId": "{organizacaoId}",
  "titulo": "Java Summit 2026",
  "descricao": "Conferencia de Java e arquitetura.",
  "local": "Centro de Convencoes",
  "fusoHorario": "America/Sao_Paulo",
  "inicioEm": "2026-10-20T12:00:00Z",
  "fimEm": "2026-10-20T21:00:00Z",
  "capacidade": 300
}
```

```http
POST /api/v1/eventos/{eventoId}/publicacao
Authorization: Bearer {token}
```

Outras transicoes: `encerramento-inscricoes`, `cancelamento` e `finalizacao`.

## Inscricao direta ou indicada

```http
POST /api/v1/publico/eventos/{slug}/inscricoes
Content-Type: application/json

{
  "nome": "Ana Souza",
  "email": "ana@example.com"
}
```

Para atribuir uma indicacao:

```json
{
  "nome": "Bruno Lima",
  "email": "bruno@example.com",
  "codigoIndicacao": "CODIGO_RECEBIDO"
}
```

A resposta devolve `tokenCancelamento` e, quando confirmada, `credencial.token`. Esses segredos aparecem apenas nesse momento.

## Convite

```http
POST /api/v1/eventos/{eventoId}/convites
Authorization: Bearer {token}
Content-Type: application/json

{
  "nome": "Carla Dias",
  "email": "carla@example.com",
  "expiraEm": "2026-10-19T12:00:00Z"
}
```

Aceite usando o mesmo endpoint de inscricao:

```json
{
  "nome": "Carla Dias",
  "email": "carla@example.com",
  "tokenConvite": "TOKEN_RECEBIDO"
}
```

## Credencial e QR Code

```http
GET /api/v1/publico/credenciais/{tokenCredencial}
GET /api/v1/publico/credenciais/{tokenCredencial}/qrcode
```

O segundo endpoint responde `image/png`. O QR Code contem somente o token opaco.

## Check-in idempotente

```http
POST /api/v1/eventos/{eventoId}/check-ins
Authorization: Bearer {tokenOperador}
Idempotency-Key: check-in-caixa-01-leitura-123
Content-Type: application/json

{
  "tokenCredencial": "TOKEN_LIDO_DO_QR"
}
```

- Primeira execucao: `201 Created`.
- Mesma chave e mesmo corpo: `200 OK` com `Idempotency-Replayed: true`.
- Mesma chave e corpo diferente: `409 Conflict`.
- Nova chave para credencial utilizada: `409 Conflict`.

## Cancelamento e promocao

```http
POST /api/v1/publico/inscricoes/{inscricaoId}/cancelamento
Content-Type: application/json

{
  "tokenCancelamento": "TOKEN_RECEBIDO_NA_INSCRICAO"
}
```

Quando havia vaga confirmada, a operacao libera capacidade e promove a primeira inscricao em espera na mesma transacao.

## Consultas

```http
GET /api/v1/publico/eventos/{slug}
GET /api/v1/publico/eventos/{slug}/ranking
GET /api/v1/eventos/{eventoId}/inscricoes?page=0&size=20
GET /api/v1/eventos/{eventoId}/check-ins?page=0&size=20
GET /api/v1/organizacoes/{organizacaoId}/auditorias?page=0&size=20
```

## Erro padrao

```json
{
  "instante": "2026-08-16T20:00:00Z",
  "status": 409,
  "codigo": "INSCRICAO_JA_EXISTE",
  "mensagem": "Este email ja esta inscrito no evento.",
  "caminho": "/api/v1/publico/eventos/java-summit/inscricoes",
  "idCorrelacao": "a0af43cd-bb08-48ec-a71f-eab3c8d6baf4",
  "violacoes": []
}
```
