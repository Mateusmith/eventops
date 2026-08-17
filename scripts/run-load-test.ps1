param(
    [ValidateRange(100, 10000)] [int] $Tentativas = 2000,
    [ValidateRange(1, 1000)] [int] $Capacidade = 100,
    [ValidateRange(1, 500)] [int] $Cancelamentos = 50
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$resultados = Join-Path $raiz 'performance\results'
$k6 = Join-Path $raiz 'performance\k6'
$monitor = Join-Path $raiz 'performance\monitor.ps1'
$api = 'http://localhost:8081'
$keycloak = 'http://localhost:18082'
$imagemK6 = 'grafana/k6:1.1.0'
$execucao = [DateTimeOffset]::UtcNow.ToString('yyyyMMddHHmmss')

if ($Tentativas -lt $Capacidade) {
    throw 'Tentativas deve ser maior ou igual a capacidade.'
}
if ($Cancelamentos -gt $Capacidade -or $Cancelamentos -gt ($Tentativas - $Capacidade)) {
    throw 'Cancelamentos deve caber tanto nas vagas confirmadas quanto na lista de espera.'
}

New-Item -ItemType Directory -Path $resultados -Force | Out-Null
Set-Location $raiz

function Obter-Token {
    $resposta = Invoke-RestMethod -Method Post `
        -Uri "$keycloak/realms/eventops/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{
            grant_type = 'password'
            client_id = 'eventops-postman'
            username = 'organizador'
            password = 'eventops123'
        }
    return $resposta.access_token
}

function Consultar-Banco([string] $sql) {
    $container = docker compose ps -q banco
    $saida = $sql | docker exec -i $container sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -F "|"'
    if ($LASTEXITCODE -ne 0) {
        throw 'Falha ao consultar o PostgreSQL durante o benchmark.'
    }
    return @($saida)
}

function Aguardar-Aplicacao {
    $limite = (Get-Date).AddMinutes(3)
    do {
        try {
            $estado = docker inspect --format '{{.State.Health.Status}}' (docker compose ps -q aplicacao)
        } catch {
            $estado = 'starting'
        }
        if ($estado -eq 'healthy') { return }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $limite)
    docker compose logs --tail 120 aplicacao
    throw 'A aplicacao nao ficou saudavel dentro do prazo.'
}

function Iniciar-Monitor([string] $cenario) {
    $arquivo = Join-Path $resultados "$cenario-monitor.csv"
    $sinal = Join-Path $resultados "$cenario.stop"
    Remove-Item -LiteralPath $arquivo, $sinal -Force -ErrorAction SilentlyContinue
    $argumentos = @(
        '-NoProfile',
        '-File', $monitor,
        '-ProjectDirectory', $raiz,
        '-OutputFile', $arquivo,
        '-StopFile', $sinal
    )
    $executavel = (Get-Process -Id $PID).Path
    $parametros = @{
        FilePath = $executavel
        ArgumentList = $argumentos
        PassThru = $true
    }
    if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) {
        $parametros.WindowStyle = 'Hidden'
    }
    $processo = Start-Process @parametros
    Start-Sleep -Seconds 2
    return [pscustomobject]@{ Processo = $processo; Arquivo = $arquivo; Sinal = $sinal }
}

function Parar-Monitor($execucaoMonitor) {
    New-Item -ItemType File -Path $execucaoMonitor.Sinal -Force | Out-Null
    if (-not $execucaoMonitor.Processo.WaitForExit(15000)) {
        $execucaoMonitor.Processo.Kill()
    }
    Remove-Item -LiteralPath $execucaoMonitor.Sinal -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path -LiteralPath $execucaoMonitor.Arquivo)) {
        throw 'O monitor nao produziu amostras de recursos.'
    }
}

function Executar-K6([string] $cenario, [string] $script, [hashtable] $variaveis) {
    $arquivoResumo = "$cenario-summary.json"
    $arquivoLog = Join-Path $resultados "$cenario-k6.log"
    $monitoramento = Iniciar-Monitor $cenario
    try {
        $argumentos = @(
            'run', '--rm', '--user', '0:0',
            '--network', ((docker inspect --format '{{range $nome, $rede := .NetworkSettings.Networks}}{{$nome}}{{end}}' (docker compose ps -q aplicacao)).Trim()),
            '--mount', "type=bind,source=$k6,target=/scripts,readonly",
            '--mount', "type=bind,source=$resultados,target=/results",
            '-e', 'BASE_URL=http://aplicacao:8080'
        )
        foreach ($item in $variaveis.GetEnumerator()) {
            $argumentos += @('-e', "$($item.Key)=$($item.Value)")
        }
        $argumentos += @(
            $imagemK6,
            'run', "--summary-export=/results/$arquivoResumo", "/scripts/$script"
        )
        & docker @argumentos 2>&1 | Tee-Object -FilePath $arquivoLog
        $codigoSaida = $LASTEXITCODE
    } finally {
        Parar-Monitor $monitoramento
    }
    if ($codigoSaida -ne 0) {
        $logsAplicacao = docker compose logs --since 15m aplicacao 2>&1
        [IO.File]::WriteAllLines(
            (Join-Path $resultados "$cenario-application.log"),
            [string[]] $logsAplicacao,
            [Text.UTF8Encoding]::new($false))
        throw "O cenario $cenario falhou no k6. Consulte $arquivoLog."
    }
}

function Maior-Valor($amostras, [string] $propriedade) {
    $valores = $amostras | ForEach-Object {
        $normalizado = ([string] $_.$propriedade).Replace(',', '.')
        [double]::Parse($normalizado, [Globalization.CultureInfo]::InvariantCulture)
    }
    return [math]::Round([double](($valores | Measure-Object -Maximum).Maximum), 2)
}

$valorAnteriorNotificacoes = $env:EVENTOPS_NOTIFICACOES_ATIVAS
$env:EVENTOPS_NOTIFICACOES_ATIVAS = 'false'

try {
    docker compose up -d --no-deps --force-recreate aplicacao | Out-Host
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao reiniciar a aplicacao para o benchmark.' }
    Aguardar-Aplicacao

    $token = Obter-Token
    $cabecalhos = @{ Authorization = "Bearer $token" }
    $organizacao = Invoke-RestMethod -Method Post -Uri "$api/api/v1/organizacoes" `
        -Headers $cabecalhos -ContentType 'application/json' `
        -Body (@{ nome = "Carga Gestao de Eventos $execucao"; documento = "CARGA-$execucao" } | ConvertTo-Json)
    $inicio = [DateTimeOffset]::UtcNow.AddDays(2)
    $evento = Invoke-RestMethod -Method Post -Uri "$api/api/v1/eventos" `
        -Headers $cabecalhos -ContentType 'application/json' `
        -Body (@{
            organizacaoId = $organizacao.id
            titulo = "Abertura de inscricoes $execucao"
            descricao = 'Evento exclusivo para benchmark reproduzivel.'
            local = 'Ambiente de carga'
            fusoHorario = 'America/Sao_Paulo'
            inicioEm = $inicio.ToString('o')
            fimEm = $inicio.AddHours(8).ToString('o')
            capacidade = $Capacidade
        } | ConvertTo-Json)
    $evento = Invoke-RestMethod -Method Post -Uri "$api/api/v1/eventos/$($evento.id)/publicacao" -Headers $cabecalhos

    Executar-K6 'registrations' 'registrations.js' @{
        TENTATIVAS = $Tentativas
        EXECUCAO = $execucao
        EVENTO_SLUG = $evento.slug
    }

    $sqlInscricoes = @"
SELECT e.vagas_ocupadas,
       COUNT(*) FILTER (WHERE i.status = 'CONFIRMADA'),
       COUNT(*) FILTER (WHERE i.status = 'LISTA_ESPERA'),
       COUNT(*) FILTER (WHERE i.status = 'CANCELADA'),
       COUNT(DISTINCT i.email_normalizado),
       COUNT(*)
  FROM eventos e
  JOIN inscricoes i ON i.evento_id = e.id
 WHERE e.id = '$($evento.id)'
 GROUP BY e.vagas_ocupadas;
"@
    $contagensIniciais = @(Consultar-Banco $sqlInscricoes)[0] -split '\|'
    if ([int]$contagensIniciais[0] -ne $Capacidade -or
        [int]$contagensIniciais[1] -ne $Capacidade -or
        [int]$contagensIniciais[2] -ne ($Tentativas - $Capacidade) -or
        [int]$contagensIniciais[4] -ne $Tentativas -or
        [int]$contagensIniciais[5] -ne $Tentativas) {
        throw "Invariantes invalidos depois das inscricoes: $($contagensIniciais -join ', ')."
    }

    $idsCancelamento = Consultar-Banco @"
SELECT id
  FROM inscricoes
 WHERE evento_id = '$($evento.id)' AND status = 'CONFIRMADA'
 ORDER BY criado_em, id
 LIMIT $Cancelamentos;
"@
    $esperadasPromocao = Consultar-Banco @"
SELECT inscricao_id
  FROM lista_espera
 WHERE evento_id = '$($evento.id)' AND status = 'AGUARDANDO'
 ORDER BY entrou_em, id
 LIMIT $Cancelamentos;
"@
    $jsonCancelamentos = ConvertTo-Json -InputObject @($idsCancelamento)
    [IO.File]::WriteAllText(
        (Join-Path $resultados 'cancellations.json'),
        $jsonCancelamentos,
        [Text.UTF8Encoding]::new($false))

    Executar-K6 'cancellations' 'cancellations.js' @{
        TOKEN_ORGANIZADOR = $token
    }

    $contagensFinais = @(Consultar-Banco $sqlInscricoes)[0] -split '\|'
    $promovidas = Consultar-Banco @"
SELECT inscricao_id
  FROM lista_espera
 WHERE evento_id = '$($evento.id)' AND status = 'PROMOVIDA'
 ORDER BY entrou_em, id;
"@
    $diferencaFila = Compare-Object -ReferenceObject @($esperadasPromocao | Sort-Object) -DifferenceObject @($promovidas | Sort-Object)
    if ([int]$contagensFinais[0] -ne $Capacidade -or
        [int]$contagensFinais[1] -ne $Capacidade -or
        [int]$contagensFinais[2] -ne ($Tentativas - $Capacidade - $Cancelamentos) -or
        [int]$contagensFinais[3] -ne $Cancelamentos -or
        $promovidas.Count -ne $Cancelamentos -or $diferencaFila) {
        throw 'As invariantes de cancelamento e promocao FIFO nao foram atendidas.'
    }

    $resumoInscricoes = Get-Content -Raw (Join-Path $resultados 'registrations-summary.json') | ConvertFrom-Json
    $resumoCancelamentos = Get-Content -Raw (Join-Path $resultados 'cancellations-summary.json') | ConvertFrom-Json
    $monitorInscricoes = Import-Csv (Join-Path $resultados 'registrations-monitor.csv')
    $monitorCancelamentos = Import-Csv (Join-Path $resultados 'cancellations-monitor.csv')

    $relatorio = [ordered]@{
        execucao = [DateTimeOffset]::UtcNow.ToString('o')
        eventoId = $evento.id
        tentativas = $Tentativas
        capacidade = $Capacidade
        confirmadas = [int]$contagensFinais[1]
        listaEspera = [int]$contagensFinais[2]
        canceladas = [int]$contagensFinais[3]
        promovidas = $promovidas.Count
        fifoValido = -not [bool]$diferencaFila
        inscricoes = [ordered]@{
            requisicoes = $resumoInscricoes.metrics.http_reqs.count
            throughputPorSegundo = [math]::Round($resumoInscricoes.metrics.http_reqs.rate, 2)
            errosPercentual = [math]::Round($resumoInscricoes.metrics.http_req_failed.value * 100, 4)
            p50Ms = [math]::Round($resumoInscricoes.metrics.http_req_duration.med, 2)
            p95Ms = [math]::Round($resumoInscricoes.metrics.http_req_duration.'p(95)', 2)
            p99Ms = [math]::Round($resumoInscricoes.metrics.http_req_duration.'p(99)', 2)
            maxMs = [math]::Round($resumoInscricoes.metrics.http_req_duration.max, 2)
            aplicacaoCpuMaxPercentual = Maior-Valor $monitorInscricoes 'aplicacao_cpu_percentual'
            aplicacaoMemoriaMaxMiB = Maior-Valor $monitorInscricoes 'aplicacao_memoria_mib'
            bancoCpuMaxPercentual = Maior-Valor $monitorInscricoes 'banco_cpu_percentual'
            bancoMemoriaMaxMiB = Maior-Valor $monitorInscricoes 'banco_memoria_mib'
            conexoesAtivasMax = Maior-Valor $monitorInscricoes 'conexoes_ativas'
            sessoesAguardandoLockMax = Maior-Valor $monitorInscricoes 'sessoes_aguardando_lock'
            locksNaoConcedidosMax = Maior-Valor $monitorInscricoes 'locks_nao_concedidos'
        }
        cancelamentos = [ordered]@{
            requisicoes = $resumoCancelamentos.metrics.http_reqs.count
            throughputPorSegundo = [math]::Round($resumoCancelamentos.metrics.http_reqs.rate, 2)
            errosPercentual = [math]::Round($resumoCancelamentos.metrics.http_req_failed.value * 100, 4)
            p50Ms = [math]::Round($resumoCancelamentos.metrics.http_req_duration.med, 2)
            p95Ms = [math]::Round($resumoCancelamentos.metrics.http_req_duration.'p(95)', 2)
            p99Ms = [math]::Round($resumoCancelamentos.metrics.http_req_duration.'p(99)', 2)
            maxMs = [math]::Round($resumoCancelamentos.metrics.http_req_duration.max, 2)
            aplicacaoCpuMaxPercentual = Maior-Valor $monitorCancelamentos 'aplicacao_cpu_percentual'
            aplicacaoMemoriaMaxMiB = Maior-Valor $monitorCancelamentos 'aplicacao_memoria_mib'
            bancoCpuMaxPercentual = Maior-Valor $monitorCancelamentos 'banco_cpu_percentual'
            bancoMemoriaMaxMiB = Maior-Valor $monitorCancelamentos 'banco_memoria_mib'
            conexoesAtivasMax = Maior-Valor $monitorCancelamentos 'conexoes_ativas'
            sessoesAguardandoLockMax = Maior-Valor $monitorCancelamentos 'sessoes_aguardando_lock'
            locksNaoConcedidosMax = Maior-Valor $monitorCancelamentos 'locks_nao_concedidos'
        }
    }
    $arquivoRelatorio = Join-Path $resultados 'load-test-result.json'
    $relatorio | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $arquivoRelatorio -Encoding UTF8
    $relatorio | ConvertTo-Json -Depth 6 | Write-Output
    Write-Host "Benchmark concluido. Relatorio: $arquivoRelatorio"
} finally {
    if ($null -eq $valorAnteriorNotificacoes) {
        Remove-Item Env:EVENTOPS_NOTIFICACOES_ATIVAS -ErrorAction SilentlyContinue
    } else {
        $env:EVENTOPS_NOTIFICACOES_ATIVAS = $valorAnteriorNotificacoes
    }
    docker compose up -d --no-deps --force-recreate aplicacao | Out-Host
    Aguardar-Aplicacao
}
