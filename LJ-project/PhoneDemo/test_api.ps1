# 测试 ppvip6.com 注册接口
# 使用您的 SOCKS5 代理

$proxy = "socks5://admin:admin@103.246.244.240:33333"
$testPhone = "01300362798"

Write-Host "正在测试注册接口..." -ForegroundColor Cyan

# 尝试多个可能的接口
$urls = @(
    "https://ppvip6.com/register",
    "https://ppvip6.com/api/register",
    "https://ppvip6.com/signup",
    "https://ppvip6.com/api/signup",
    "https://ppvip6.com/api/user/register"
)

foreach ($url in $urls) {
    Write-Host "`n测试: $url" -ForegroundColor Yellow
    
    try {
        $response = curl -x $proxy `
            -X POST `
            -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" `
            -H "Accept: application/json" `
            -H "Content-Type: application/x-www-form-urlencoded" `
            -d "mobile=$testPhone&phone=$testPhone&username=test362798&password=Test123456&password_confirmation=Test123456" `
            --max-time 10 `
            -i `
            $url 2>&1
        
        Write-Host $response -ForegroundColor Green
    } catch {
        Write-Host "请求失败: $_" -ForegroundColor Red
    }
}
