<#
.SYNOPSIS
  THLuxury — kịch bản demo end-to-end qua API Gateway.
  Mô phỏng hành trình khách hàng: đăng ký -> đăng nhập -> xem sản phẩm ->
  thêm giỏ -> đặt hàng -> xem đơn -> chat AI.

.USAGE
  pwsh ./scripts/demo.ps1                 # mặc định http://localhost:8080
  pwsh ./scripts/demo.ps1 -BaseUrl http://localhost:8080

  Yêu cầu: stack đã chạy (docker compose up) và đã seed dữ liệu (scripts/seed.sh).
#>
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Email   = "demo@thluxury.local",
  [string]$Password = "Demo@1234"
)

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$global:Token = $null

function Step($msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Info($msg) { Write-Host "  $msg" -ForegroundColor Gray }

function Api {
  param(
    [string]$Method,
    [string]$Path,
    [object]$Body,
    [switch]$Auth
  )
  $headers = @{ "Content-Type" = "application/json" }
  if ($Auth -and $global:Token) { $headers["Authorization"] = "Bearer $($global:Token)" }
  $uri = "$BaseUrl$Path"
  if ($Body) {
    $json = ($Body | ConvertTo-Json -Depth 8)
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body $json
  } else {
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
  }
}

Write-Host "THLuxury demo -> $BaseUrl" -ForegroundColor Yellow

# 1) Đăng ký (bỏ qua nếu đã tồn tại)
Step "1. Đăng ký khách hàng ($Email)"
try {
  Api POST "/api/auth/register" @{ email = $Email; password = $Password; fullName = "Demo User"; phone = "0900000000" } | Out-Null
  Ok "Đã tạo tài khoản"
} catch {
  Info "Tài khoản có thể đã tồn tại — bỏ qua ($($_.Exception.Message))"
}

# 2) Đăng nhập
Step "2. Đăng nhập"
$login = Api POST "/api/auth/login" @{ email = $Email; password = $Password }
$global:Token = $login.accessToken
$userId = $login.user.id
Ok "accessToken nhận được; userId=$userId"

# 3) Danh sách chi nhánh
Step "3. Lấy chi nhánh"
$branches = Api GET "/api/branches"
$branch = if ($branches -is [array]) { $branches[0] } elseif ($branches.content) { $branches.content[0] } else { $branches }
$branchId = $branch.id
Ok "Chi nhánh: $($branch.name) ($branchId)"

# 4) Danh sách sản phẩm
Step "4. Xem sản phẩm"
$products = Api GET "/api/products?page=0&size=12"
$list = if ($products.content) { $products.content } else { $products }
Ok "Nhận $($list.Count) sản phẩm (chọn sản phẩm còn hàng tại chi nhánh để đặt)"

# 5+6) Thêm giỏ + đặt hàng — thử lần lượt cho tới khi gặp sản phẩm còn tồn kho
Step "5+6. Thêm vào giỏ & đặt hàng (STORE_PICKUP / COD)"
$order = $null
foreach ($product in $list) {
  try { Api DELETE "/api/cart" -Auth | Out-Null } catch {}
  Api POST "/api/cart/sync" @{ items = @(@{ productId = $product.id; quantity = 1 }) } -Auth | Out-Null
  try {
    $order = Api POST "/api/orders/checkout" @{
      deliveryType  = "STORE_PICKUP"
      branchId      = $branchId
      paymentMethod = "COD"
      customer      = @{ fullName = "Demo User"; phone = "0900000000"; email = $Email }
    } -Auth
    Ok "Sản phẩm '$($product.tenSp)' còn hàng → Đơn $($order.maDh), trạng thái $($order.currentStatus), tổng $($order.total)"
    break
  } catch {
    $code = $null; if ($_.Exception.Response) { $code = $_.Exception.Response.StatusCode.value__ }
    Info "Sản phẩm '$($product.tenSp)' không đặt được (HTTP $code) — thử sản phẩm khác"
  }
}
if (-not $order) { throw "Không đặt được đơn nào — kiểm tra seed tồn kho (scripts/seed.sh)." }

# 7) Xem chi tiết đơn
Step "7. Xem chi tiết đơn"
$detail = Api GET "/api/orders/me/$($order.maDh)" -Auth
Ok "VAT $($detail.vatAmount), giảm $($detail.discountAmount), tổng $($detail.total)"

# 8) Chat AI
Step "8. Chat với trợ lý AI"
try {
  $chat = Api POST "/api/ai/chat" @{ sessionId = "demo-session"; userId = $userId; message = "Tư vấn nhẫn kim cương dưới 20 triệu" }
  Ok "Intent: $($chat.intent)"
  Info "Trả lời: $($chat.reply)"
} catch {
  Info "AI service chưa sẵn sàng hoặc thiếu GEMINI_API_KEY — bỏ qua ($($_.Exception.Message))"
}

Step "Hoàn tất"
Write-Host @"
  Storefront : http://localhost:3000
  Admin      : http://localhost:5173
  MailHog    : http://localhost:8025   (email xác nhận đơn / reset mật khẩu)
  RabbitMQ   : http://localhost:15672  (admin/admin)
  Grafana    : http://localhost:3001   (admin/admin)  -> THLuxury dashboards
  Prometheus : http://localhost:9090
"@ -ForegroundColor Yellow
