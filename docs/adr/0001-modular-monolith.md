# ADR 0001: Monolito modular

Status: aceito.

## Contexto

Inscricao, capacidade, fila e credencial precisam mudar de forma consistente. Microsservicos exigiriam saga e reconciliacao desde a primeira versao.

## Decisao

Usar um unico deploy com modulos de dominio verificados pelo Spring Modulith. Cada modulo expoe apenas tipos no pacote raiz e esconde persistencia em `internal`.

## Consequencias

Transacoes permanecem locais e a operacao e simples. Um modulo podera ser extraido futuramente quando volume, equipe ou isolamento justificarem o custo distribuido.
