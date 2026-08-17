$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
$arquivoAmbiente = Join-Path $raiz '.env'
$segredoObservabilidade = Join-Path $raiz 'secrets\observability_password.txt'

if (-not (Test-Path $arquivoAmbiente)) {
    Copy-Item (Join-Path $raiz '.env.example') $arquivoAmbiente
    Write-Host 'Arquivo .env criado a partir de .env.example.'
}

if (-not (Test-Path $segredoObservabilidade)) {
    Copy-Item (Join-Path $raiz 'secrets\observability_password.example.txt') $segredoObservabilidade
    Write-Host 'Segredo local do Prometheus criado na pasta secrets.'
}

Write-Host 'Ambiente local preparado. Troque os valores demonstrativos antes de compartilhar a instalacao.'
