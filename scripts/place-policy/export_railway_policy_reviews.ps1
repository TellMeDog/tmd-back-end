param(
    [string]$ContainerName = "tmd_mysql",
    [string]$EnvFile = ".env",
    [string]$HostName,
    [int]$Port,
    [string]$OutputPath = "exports/place_pet_policy_pending_reviews.csv"
)

$ErrorActionPreference = "Stop"
$variables = @{}
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^(RAILWAY_DB_[A-Z_]+)=(.*)$') {
        $variables[$matches[1]] = $matches[2]
    }
}

$env:MYSQL_PWD = $variables["RAILWAY_DB_PASSWORD"]
$railwayHost = if ($HostName) { $HostName } else { $variables["RAILWAY_DB_HOST"] }
$railwayPort = if ($Port) { $Port } else { [int]$variables["RAILWAY_DB_PORT"] }
if ([string]::IsNullOrWhiteSpace($railwayHost) -or $railwayPort -le 0) {
    throw "Railway DB host/port is required. Set RAILWAY_DB_HOST and RAILWAY_DB_PORT in the env file or pass -HostName and -Port."
}
$query = @"
SELECT JSON_OBJECT(
  'review_id', r.id,
  'place_id', p.id,
  'content_id', p.content_id,
  'title', p.title,
  'source_modified_time', r.source_modified_time,
  'review_reasons', r.review_reasons,
  'suggested_policy_json', r.suggested_policy_json,
  'access_scope', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.accessScope')),
  'all_breeds_allowed', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.allBreedsAllowed')),
  'dangerous_breed_allowed', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.dangerousBreedAllowed')),
  'dangerous_breed_allowed_condition', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.dangerousBreedAllowedCondition')),
  'max_weight_kg', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.maxWeightKg')),
  'weight_limit_type', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.weightLimitType')),
  'leash_required', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.leashRequired')),
  'muzzle_required', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.muzzleRequired')),
  'kennel_required', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.kennelRequired')),
  'advance_inquiry_required', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.advanceInquiryRequired')),
  'max_pet_count', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.maxPetCount')),
  'default_policy', JSON_UNQUOTE(JSON_EXTRACT(r.suggested_policy_json, '$.defaultPolicy')),
  'resolution', 'APPLY',
  'review_note', '',
  'acmpy_type_cd', i.acmpy_type_cd,
  'acmpy_psbl_cpam', i.acmpy_psbl_cpam,
  'acmpy_need_mtr', i.acmpy_need_mtr,
  'rela_acdnt_risk_mtr', i.rela_acdnt_risk_mtr,
  'etc_acmpy_info', i.etc_acmpy_info
)
FROM place_pet_policy_review r
JOIN place p ON p.id = r.place_id
LEFT JOIN place_pet_info i ON i.place_id = p.id
WHERE r.status = 'PENDING'
ORDER BY r.id
"@

$lines = docker exec --env MYSQL_PWD $ContainerName mysql `
    -h $railwayHost -P $railwayPort `
    -u $variables["RAILWAY_DB_USERNAME"] $variables["RAILWAY_DB_NAME"] `
    --default-character-set=utf8mb4 --batch --raw --skip-column-names -e $query
if ($LASTEXITCODE -ne 0) { throw "Railway 검토 대기 정책 조회에 실패했습니다." }

$rows = @($lines | ForEach-Object { $_ | ConvertFrom-Json })
$fullOutputPath = [IO.Path]::GetFullPath($OutputPath)
New-Item -ItemType Directory -Force (Split-Path -Parent $fullOutputPath) | Out-Null
$rows | Export-Csv -LiteralPath $fullOutputPath -NoTypeInformation -Encoding utf8
Write-Output "Pending policy reviews exported: $fullOutputPath ($($rows.Count) rows)"
