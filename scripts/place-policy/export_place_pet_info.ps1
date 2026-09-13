param(
    [string]$ContainerName = "tmd_mysql",
    [string]$Database = "tmd",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repositoryRoot ".policy-work\place_pet_info.jsonl"
}
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)

$containerJson = docker inspect $ContainerName
if ($LASTEXITCODE -ne 0) {
    throw "Docker container '$ContainerName'을 찾을 수 없습니다."
}

$container = $containerJson | ConvertFrom-Json
$passwordEntry = $container[0].Config.Env |
    Where-Object { $_ -like "MYSQL_ROOT_PASSWORD=*" } |
    Select-Object -First 1

if ([string]::IsNullOrWhiteSpace($passwordEntry)) {
    throw "Container에서 MYSQL_ROOT_PASSWORD를 찾을 수 없습니다."
}

$databasePassword = $passwordEntry.Substring("MYSQL_ROOT_PASSWORD=".Length)
$query = @"
SELECT JSON_OBJECT(
    'id', ppi.id,
    'place_id', ppi.place_id,
    'content_id', p.content_id,
    'acmpy_need_mtr', ppi.acmpy_need_mtr,
    'acmpy_psbl_cpam', ppi.acmpy_psbl_cpam,
    'acmpy_type_cd', ppi.acmpy_type_cd,
    'etc_acmpy_info', ppi.etc_acmpy_info,
    'rela_acdnt_risk_mtr', ppi.rela_acdnt_risk_mtr,
    'rela_frnsh_prdlst', ppi.rela_frnsh_prdlst,
    'rela_poses_fclty', ppi.rela_poses_fclty,
    'rela_purc_prdlst', ppi.rela_purc_prdlst,
    'rela_rntl_prdlst', ppi.rela_rntl_prdlst,
    'status', ppi.status
)
FROM place_pet_info ppi
JOIN place p ON p.id = ppi.place_id
ORDER BY ppi.id
"@

$lines = docker exec --env "MYSQL_PWD=$databasePassword" $ContainerName mysql `
    -uroot "-D$Database" `
    --default-character-set=utf8mb4 --batch --raw --skip-column-names `
    -e $query

if ($LASTEXITCODE -ne 0) {
    throw "place_pet_info 추출에 실패했습니다."
}
if ($lines.Count -eq 0) {
    throw "추출 결과가 비어 있습니다. 기존 파일을 덮어쓰지 않습니다."
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$temporaryPath = "$OutputPath.tmp"
[System.IO.File]::WriteAllLines(
    $temporaryPath,
    [string[]]$lines,
    [System.Text.UTF8Encoding]::new($false)
)
Move-Item -LiteralPath $temporaryPath -Destination $OutputPath -Force

Write-Output "PlacePetInfo JSONL exported: $OutputPath ($($lines.Count) rows)"
