# ADR 0002: Capacidade atomica

Status: aceito.

## Contexto

Contar inscricoes e depois inserir permite ultrapassar a capacidade quando duas requisicoes observam a mesma ultima vaga.

## Decisao

Manter `vagas_ocupadas` no evento e reserva-la com um unico `UPDATE` condicional. A restricao do banco impede ocupacao negativa ou superior a capacidade.

## Consequencias

A inscricao ganha consistencia sob concorrencia sem bloquear toda a tabela. Cancelamento e promocao precisam atualizar o contador na mesma transacao.
