$ErrorActionPreference = 'Stop'

$api = 'http://localhost:8081'
$keycloak = 'http://localhost:18082'
$sufixo = [Guid]::NewGuid().ToString('N').Substring(0, 8)

function Obter-Token([string] $usuario) {
    $resposta = Invoke-RestMethod -Method Post `
        -Uri "$keycloak/realms/eventops/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{
            grant_type = 'password'
            client_id = 'eventops-postman'
            client_secret = 'eventops-postman-secret'
            username = $usuario
            password = 'eventops123'
        }
    return $resposta.access_token
}

function Cabecalho([string] $token) {
    return @{ Authorization = "Bearer $token" }
}

function Exigir([bool] $condicao, [string] $mensagem) {
    if (-not $condicao) {
        throw "Validacao falhou: $mensagem"
    }
}

$tokenOrganizador = Obter-Token 'organizador'
$tokenOperador = Obter-Token 'operador'

$organizacao = Invoke-RestMethod -Method Post -Uri "$api/api/v1/organizacoes" `
    -Headers (Cabecalho $tokenOrganizador) -ContentType 'application/json' `
    -Body (@{ nome = "EventOps Demo $sufixo"; documento = "DEMO-$sufixo" } | ConvertTo-Json)

$null = Invoke-RestMethod -Method Post -Uri "$api/api/v1/organizacoes/$($organizacao.id)/membros" `
    -Headers (Cabecalho $tokenOrganizador) -ContentType 'application/json' `
    -Body (@{
        nome = 'Carlos Check-in'
        email = 'operador@eventops.local'
        papel = 'OPERADOR_CHECKIN'
    } | ConvertTo-Json)

$inicio = (Get-Date).ToUniversalTime().AddDays(2)
$evento = Invoke-RestMethod -Method Post -Uri "$api/api/v1/eventos" `
    -Headers (Cabecalho $tokenOrganizador) -ContentType 'application/json' `
    -Body (@{
        organizacaoId = $organizacao.id
        titulo = "Java Summit $sufixo"
        descricao = 'Evento criado pelo teste real do EventOps.'
        local = 'Centro de Convencoes'
        fusoHorario = 'America/Sao_Paulo'
        inicioEm = $inicio.ToString('o')
        fimEm = $inicio.AddHours(8).ToString('o')
        capacidade = 1
    } | ConvertTo-Json)

$evento = Invoke-RestMethod -Method Post -Uri "$api/api/v1/eventos/$($evento.id)/publicacao" `
    -Headers (Cabecalho $tokenOrganizador)

$ana = Invoke-RestMethod -Method Post -Uri "$api/api/v1/publico/eventos/$($evento.slug)/inscricoes" `
    -ContentType 'application/json' `
    -Body (@{ nome = 'Ana Souza'; email = "ana.$sufixo@example.com" } | ConvertTo-Json)

$bruno = Invoke-RestMethod -Method Post -Uri "$api/api/v1/publico/eventos/$($evento.slug)/inscricoes" `
    -ContentType 'application/json' `
    -Body (@{
        nome = 'Bruno Lima'
        email = "bruno.$sufixo@example.com"
        codigoIndicacao = $ana.codigoIndicacao
    } | ConvertTo-Json)

Exigir ($ana.status -eq 'CONFIRMADA') 'a primeira inscricao deveria ser confirmada'
Exigir ($bruno.status -eq 'LISTA_ESPERA') 'a segunda inscricao deveria entrar na fila'

$chaveIdempotencia = "check-in-$([Guid]::NewGuid())"
$corpoCheckIn = @{ tokenCredencial = $ana.credencial.token } | ConvertTo-Json
$primeiroCheckIn = Invoke-WebRequest -Method Post -Uri "$api/api/v1/eventos/$($evento.id)/check-ins" `
    -Headers ((Cabecalho $tokenOperador) + @{ 'Idempotency-Key' = $chaveIdempotencia }) `
    -ContentType 'application/json' -Body $corpoCheckIn
$repeticaoCheckIn = Invoke-WebRequest -Method Post -Uri "$api/api/v1/eventos/$($evento.id)/check-ins" `
    -Headers ((Cabecalho $tokenOperador) + @{ 'Idempotency-Key' = $chaveIdempotencia }) `
    -ContentType 'application/json' -Body $corpoCheckIn

Exigir ($primeiroCheckIn.StatusCode -eq 201) 'o primeiro check-in deveria retornar 201'
Exigir ($repeticaoCheckIn.StatusCode -eq 200) 'a repeticao deveria retornar 200'
Exigir ($repeticaoCheckIn.Headers['Idempotency-Replayed'] -contains 'true') 'a repeticao deveria ser identificada'

$cancelamento = Invoke-RestMethod -Method Post `
    -Uri "$api/api/v1/publico/inscricoes/$($ana.id)/cancelamento" `
    -ContentType 'application/json' `
    -Body (@{ tokenCancelamento = $ana.tokenCancelamento } | ConvertTo-Json)

Exigir ($cancelamento.inscricaoPromovidaId -eq $bruno.id) 'a primeira pessoa da fila deveria ser promovida'

$publico = Invoke-RestMethod -Uri "$api/api/v1/publico/eventos/$($evento.slug)"
$auditorias = Invoke-RestMethod -Uri "$api/api/v1/organizacoes/$($organizacao.id)/auditorias" `
    -Headers (Cabecalho $tokenOrganizador)

Exigir ($publico.vagasOcupadas -eq 1) 'a ocupacao deveria permanecer em uma vaga apos a promocao'
Exigir ($auditorias.totalElementos -gt 0) 'a trilha de auditoria deveria possuir registros'

[pscustomobject]@{
    resultado = 'SUCESSO'
    organizacaoId = $organizacao.id
    eventoId = $evento.id
    slug = $evento.slug
    inscricaoConfirmada = $ana.id
    inscricaoPromovida = $bruno.id
    checkInOriginal = $primeiroCheckIn.StatusCode
    checkInRepetido = $repeticaoCheckIn.StatusCode
    auditorias = $auditorias.totalElementos
} | Format-List
