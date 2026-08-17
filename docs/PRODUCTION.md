# Preparacao para producao

O perfil `production` remove os valores convenientes do laboratorio e valida configuracoes criticas antes de aceitar trafego.

## Segredos obrigatorios

Forneca as propriedades abaixo por um gerenciador de segredos da plataforma. Nao grave valores reais em `.env`, imagem Docker, pipeline ou manifesto versionado.

| Variavel | Finalidade |
|---|---|
| `EVENTOPS_BANCO_URL` | JDBC do PostgreSQL privado |
| `EVENTOPS_BANCO_USUARIO` | Usuario de menor privilegio da aplicacao |
| `EVENTOPS_BANCO_SENHA` | Senha exclusiva do banco |
| `EVENTOPS_EMISSOR_JWT` | Issuer HTTPS do provedor OIDC |
| `EVENTOPS_URL_CHAVES_JWK` | Endpoint HTTPS das chaves publicas |
| `EVENTOPS_EMAIL_HOST` | Servidor SMTP |
| `EVENTOPS_EMAIL_USUARIO` | Conta SMTP exclusiva |
| `EVENTOPS_EMAIL_SENHA` | Segredo SMTP |
| `EVENTOPS_URL_PUBLICA` | URL HTTPS usada nos links enviados |
| `EVENTOPS_OBSERVABILIDADE_SENHA` | Senha exclusiva do scrape, quando nao houver `configtree` |

Ative com `SPRING_PROFILES_ACTIVE=production`. Senhas validadas pela aplicacao devem ter pelo menos 16 caracteres.
OpenAPI e Swagger ficam desativados por padrao nesse perfil; habilite-os conscientemente apenas em uma rede administrativa com `EVENTOPS_OPENAPI_ATIVA=true` e `EVENTOPS_SWAGGER_ATIVO=true`.

## Rede

- Publique somente a porta da API, normalmente `8080`, atras de TLS e WAF ou gateway.
- Mantenha `management.server.port`, por padrao `9090`, em uma rede acessivel apenas pelo Prometheus e pelas sondas da plataforma.
- Nao publique PostgreSQL, SMTP ou console administrativo do provedor de identidade na Internet.
- Restrinja a saida da aplicacao aos destinos realmente usados.

## Identidade

- Aplicacoes de navegador usam Authorization Code com PKCE.
- Integracoes sem usuario usam Client Credentials, escopos minimos e credenciais distintas.
- Desative Direct Access Grants, usuarios demonstrativos e o cliente `eventops-postman`.
- Configure expiracao curta para access tokens, rotacao de refresh tokens, MFA para administradores e registro de eventos administrativos.

## Validacao da entrega

1. Inicie uma replica nova e confirme que configuracao fraca impede o startup.
2. Verifique `401` nas metricas sem credencial, `403` com papel incorreto e `200` somente para o Prometheus.
3. Confirme que a porta `9090` nao responde fora da rede operacional.
4. Execute migrations antes de liberar trafego e valide rollback da versao da aplicacao.
5. Acompanhe alvo Prometheus, erros HTTP, fila de notificacoes e saturacao do pool JDBC.

O procedimento de incidente esta em [runbooks/credential-compromise.md](runbooks/credential-compromise.md).
