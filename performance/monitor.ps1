param(
    [Parameter(Mandatory = $true)] [string] $ProjectDirectory,
    [Parameter(Mandatory = $true)] [string] $OutputFile,
    [Parameter(Mandatory = $true)] [string] $StopFile,
    [int] $IntervalMilliseconds = 250
)

$ErrorActionPreference = 'Stop'
Set-Location $ProjectDirectory

$containerAplicacao = docker compose ps -q aplicacao
$containerBanco = docker compose ps -q banco
if (-not $containerAplicacao -or -not $containerBanco) {
    throw 'Os containers da aplicacao e do banco precisam estar em execucao.'
}

function Converter-Numero([string] $valor) {
    $normalizado = ($valor -replace '[^0-9,.]', '').Replace(',', '.')
    return [double]::Parse($normalizado, [Globalization.CultureInfo]::InvariantCulture)
}

function Converter-MemoriaMiB([string] $valor) {
    $uso = ($valor -split '/')[0].Trim()
    if ($uso -notmatch '^([0-9.]+)(KiB|MiB|GiB)$') {
        return 0
    }
    $numero = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
    switch ($Matches[2]) {
        'KiB' { return $numero / 1024 }
        'GiB' { return $numero * 1024 }
        default { return $numero }
    }
}

Remove-Item -LiteralPath $OutputFile -Force -ErrorAction SilentlyContinue

while (-not (Test-Path -LiteralPath $StopFile)) {
    try {
        $estatisticas = docker stats --no-stream --format '{{json .}}' $containerAplicacao $containerBanco |
            ForEach-Object { $_ | ConvertFrom-Json }
        $aplicacao = $estatisticas | Where-Object { $_.Container -eq $containerAplicacao.Substring(0, 12) -or $_.Name -like '*aplicacao*' }
        $banco = $estatisticas | Where-Object { $_.Container -eq $containerBanco.Substring(0, 12) -or $_.Name -like '*banco*' }

        $sql = @'
SELECT COUNT(*) FILTER (WHERE pid <> pg_backend_pid() AND state = 'active'),
       COUNT(*) FILTER (WHERE pid <> pg_backend_pid() AND wait_event_type = 'Lock'),
       (SELECT COUNT(*) FROM pg_locks WHERE NOT granted)
  FROM pg_stat_activity
 WHERE datname = current_database();
'@
        $linhaBanco = $sql | docker exec -i $containerBanco sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -F "|"'
        $valoresBanco = $linhaBanco.Trim() -split '\|'

        [pscustomobject]@{
            instante = [DateTimeOffset]::UtcNow.ToString('o')
            aplicacao_cpu_percentual = Converter-Numero $aplicacao.CPUPerc
            aplicacao_memoria_mib = [math]::Round((Converter-MemoriaMiB $aplicacao.MemUsage), 2)
            banco_cpu_percentual = Converter-Numero $banco.CPUPerc
            banco_memoria_mib = [math]::Round((Converter-MemoriaMiB $banco.MemUsage), 2)
            conexoes_ativas = [int] $valoresBanco[0]
            sessoes_aguardando_lock = [int] $valoresBanco[1]
            locks_nao_concedidos = [int] $valoresBanco[2]
        } | Export-Csv -LiteralPath $OutputFile -Append -NoTypeInformation -Encoding UTF8
    } catch {
        # Uma amostra perdida nao invalida a execucao; o orquestrador exige amostras validas ao final.
    }
    Start-Sleep -Milliseconds $IntervalMilliseconds
}
