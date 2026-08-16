# ADR 0003: Credenciais opacas

Status: aceito.

## Contexto

Um QR Code com ID previsivel permite enumeracao. Um JWT longo dificulta revogacao imediata e expõe metadados ao portador.

## Decisao

Gerar 256 bits aleatorios, entregar o token uma vez e persistir apenas SHA-256. O banco mantem o estado `ATIVA`, `UTILIZADA` ou `REVOGADA`.

## Consequencias

Credenciais sao revogaveis e nao revelam dados. A validacao exige consulta ao servidor, apropriada para o check-in online deste escopo.
