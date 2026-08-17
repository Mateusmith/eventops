# ADR 0005 - Seguranca operacional e observabilidade

## Contexto

A API usa JWT para o dominio, mas Prometheus precisa acessar o Actuator sem receber privilegios de negocio. Publicar metricas sem autenticacao ou manter senhas no Compose cria uma superficie desnecessaria e facilita o vazamento de configuracoes demonstrativas para ambientes reais.

## Decisao

- O Actuator possui uma cadeia de seguranca propria e precede a cadeia OAuth2 da API.
- Apenas `health` e publico. Os demais endpoints exigem HTTP Basic e o papel interno `OBSERVABILIDADE`.
- No Docker, a porta de gerenciamento `9090` existe somente na rede interna e nao e publicada no host.
- Prometheus recebe a mesma senha por Docker secret; a aplicacao a le por `configtree`.
- PostgreSQL, Keycloak e Grafana recebem credenciais de `.env`, arquivo ignorado pelo Git.
- O perfil `production` falha na inicializacao diante de senha curta ou demonstrativa, URL publica sem HTTPS, emissor OIDC sem HTTPS ou Actuator na porta da API.
- O cliente Postman local e publico e usa password grant apenas para facilitar o laboratorio. Producao deve usar Authorization Code com PKCE para usuarios e Client Credentials para integracoes de maquina.

## Consequencias

O ambiente local exige uma etapa explicita de preparacao, executada por `scripts/setup-local.ps1`. O scrape passa a depender de uma credencial rotacionavel e um erro nessa credencial deixa o alvo `DOWN`, mas nao derruba a API. A separacao de portas permite regras de rede independentes para trafego de negocio e telemetria.
