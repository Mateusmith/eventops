# Contribuindo

1. Abra uma issue descrevendo problema, regra e criterio de aceite.
2. Crie uma branch curta a partir de `main`.
3. Preserve os limites dos modulos e escreva migrations apenas aditivas.
4. Execute `./mvnw verify` e `docker compose config --quiet`.
5. Atualize OpenAPI, Postman e documentacao quando o contrato mudar.
6. Abra um pull request com evidencias de teste.

Commits seguem Conventional Commits, por exemplo `feat: adicionar lote de convites` e `fix: serializar ultima vaga`.

Nunca inclua tokens, credenciais reais, dumps ou dados pessoais. Os segredos existentes no Compose sao exclusivamente demonstrativos.
