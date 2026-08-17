# Runbook - comprometimento de credencial

## Objetivo

Conter, rotacionar e investigar o vazamento de uma senha, token ou credencial OIDC sem apagar evidencias.

## Resposta imediata

1. Registre horario, ambiente, credencial afetada e origem do alerta em um incidente privado.
2. Revogue primeiro a credencial comprometida no sistema emissor.
3. Gere uma credencial nova, atualize o gerenciador de segredos e reinicie as replicas gradualmente.
4. Confirme autenticacao da aplicacao e mantenha a credencial anterior revogada.
5. Procure uso anormal nos logs de identidade, banco, gateway, auditoria e Prometheus.

## Por tipo

| Credencial | Acao principal | Evidencia de recuperacao |
|---|---|---|
| Cliente OIDC | Revogar sessoes, rotacionar segredo ou chave e reduzir escopos | Tokens antigos rejeitados e novo fluxo autenticando |
| Banco | Criar senha nova ou usuario substituto e encerrar sessoes antigas | Pool conectado somente com a nova credencial |
| Prometheus | Trocar o secret nos dois workloads e reiniciar o scrape | Alvo `UP` e senha anterior retornando `401` |
| Grafana | Rotacionar administrador e tokens de service account | Login antigo negado e dashboards acessiveis |
| SMTP | Revogar token no provedor e revisar volume enviado | Nova notificacao entregue sem envio suspeito |

## Encerramento

Preserve logs, documente a causa raiz, identifique o alcance dos dados, abra acoes corretivas e avalie obrigacoes de notificacao. Nunca publique tokens ou dados pessoais na issue do GitHub.
