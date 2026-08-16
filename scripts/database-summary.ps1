$ErrorActionPreference = 'Stop'

docker compose exec -T banco psql -U eventops -d eventops -c @'
SELECT 'organizacoes' AS tabela, COUNT(*) AS registros FROM organizacoes
UNION ALL SELECT 'eventos', COUNT(*) FROM eventos
UNION ALL SELECT 'inscricoes', COUNT(*) FROM inscricoes
UNION ALL SELECT 'lista_espera', COUNT(*) FROM lista_espera
UNION ALL SELECT 'convites', COUNT(*) FROM convites
UNION ALL SELECT 'credenciais', COUNT(*) FROM credenciais
UNION ALL SELECT 'check_ins', COUNT(*) FROM check_ins
UNION ALL SELECT 'notificacoes', COUNT(*) FROM notificacoes
UNION ALL SELECT 'eventos_outbox', COUNT(*) FROM eventos_outbox
UNION ALL SELECT 'auditorias', COUNT(*) FROM auditorias
ORDER BY tabela;
'@
