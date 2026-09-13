param(
    [string]$ContainerName = "tmd_mysql",
    [string]$Database = "tmd",
    [string]$OutputDirectory = "",
    [string]$ExistingPolicy = "",
    [string]$ExistingReview = "",
    [switch]$Full
)

$ErrorActionPreference = "Stop"
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$sourcePath = Join-Path $repositoryRoot ".policy-work\place_pet_info.jsonl"

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $repositoryRoot "exports"
}

& (Join-Path $PSScriptRoot "export_place_pet_info.ps1") `
    -ContainerName $ContainerName `
    -Database $Database `
    -OutputPath $sourcePath

if ($LASTEXITCODE -ne 0) {
    throw "PlacePetInfo 원문 추출에 실패했습니다."
}

$arguments = @(
    (Join-Path $PSScriptRoot "generate_policy.py"),
    "--source", $sourcePath,
    "--output-dir", $OutputDirectory
)

if ($Full) {
    $arguments += "--full"
} else {
    if ([string]::IsNullOrWhiteSpace($ExistingPolicy)) {
        $candidate = Join-Path $OutputDirectory "place_pet_policy.csv"
        if (Test-Path -LiteralPath $candidate) {
            $ExistingPolicy = $candidate
        }
    }
    if ([string]::IsNullOrWhiteSpace($ExistingReview)) {
        $candidate = Join-Path $OutputDirectory "place_pet_policy_review.csv"
        if (Test-Path -LiteralPath $candidate) {
            $ExistingReview = $candidate
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($ExistingPolicy)) {
        $arguments += @("--existing-policy", $ExistingPolicy)
    }
    if (-not [string]::IsNullOrWhiteSpace($ExistingReview)) {
        $arguments += @("--existing-review", $ExistingReview)
    }
}

python @arguments
if ($LASTEXITCODE -ne 0) {
    throw "PlacePetPolicy CSV 생성에 실패했습니다."
}
