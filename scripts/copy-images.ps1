# ============================================================
# THLuxury — Copy 109 ảnh sản phẩm từ 5TLuxury sang storefront/public
# Mapping: 5TLuxury/frontend/src/image/{X.Y}.png
#       -> frontend/storefront/public/products/{X.Y}.png
# Catalog seed sẽ dùng URL tương đối /products/{X.Y}.png.
# ============================================================
$ErrorActionPreference = 'Stop'

$repoRoot   = Split-Path $PSScriptRoot -Parent
$sourceDir  = Join-Path $repoRoot '5TLuxury\frontend\src\image'
$targetDir  = Join-Path $repoRoot 'frontend\storefront\public\products'

if (-not (Test-Path $sourceDir)) {
    Write-Error "Source folder không tồn tại: $sourceDir"
    exit 1
}

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    Write-Host "✓ Tạo $targetDir"
}

$files = Get-ChildItem -Path $sourceDir -Filter *.png
Write-Host "→ Đang copy $($files.Count) ảnh..."

foreach ($f in $files) {
    Copy-Item -Path $f.FullName -Destination (Join-Path $targetDir $f.Name) -Force
}

Write-Host "✓ Đã copy $($files.Count) ảnh vào $targetDir"
