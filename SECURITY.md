# Seguranca

## Relato responsavel

Nao abra issue publica para vulnerabilidades. Em um repositorio hospedado, use um GitHub Security Advisory privado e informe impacto, passos de reproducao e versao afetada.

## Modelo local

O Compose le credenciais de `.env` e o segredo de scrape de um arquivo ignorado pelo Git. Os valores de `.env.example` sao apenas marcadores demonstrativos. O cliente Postman local e publico e nao possui segredo para ser confundido com uma credencial de producao.

Tokens de credencial, convite e cancelamento devem ser tratados como segredos. A aplicacao armazena somente hashes, mas URLs podem aparecer no historico do navegador ou em logs de proxy; configure redacao de caminho na borda.

## Producao e rotacao

Use TLS, gerenciador de segredos, SMTP autenticado e banco sem porta publica. O perfil `production` rejeita senhas fracas, URLs sem HTTPS e Actuator na porta publica da API. Clientes OIDC devem ter escopo minimo e rotacao documentada; revogue a credencial anterior antes de encerrar um incidente.

Consulte [Preparacao para producao](docs/PRODUCTION.md) e o [runbook de comprometimento](docs/runbooks/credential-compromise.md).

## Escopo suportado

Correcao de vulnerabilidades se concentra na versao mais recente da branch `main`.
