# Seguranca

## Relato responsavel

Nao abra issue publica para vulnerabilidades. Em um repositorio hospedado, use um GitHub Security Advisory privado e informe impacto, passos de reproducao e versao afetada.

## Modelo local

As senhas e o segredo do cliente presentes no Compose sao dados de demonstracao. Troque-os em qualquer ambiente compartilhado. Use TLS, cofre de segredos, SMTP autenticado e banco sem porta publica em producao.

Tokens de credencial, convite e cancelamento devem ser tratados como segredos. A aplicacao armazena somente hashes, mas URLs podem aparecer no historico do navegador ou em logs de proxy; configure redacao de caminho na borda.

## Escopo suportado

Correcao de vulnerabilidades se concentra na versao mais recente da branch `main`.
