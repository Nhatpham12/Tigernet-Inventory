<#
.SYNOPSIS
    Tạo database riêng cho nhánh dev (mặc định: tigernet_dev) từ docs/Database_Script_v7.sql.

.DESCRIPTION
    Đọc script gộp v7, đổi tên database "tigernet" thành tên database dev, rồi chạy bằng mysql.exe.
    Database tigernet (bản chính) không bị đụng tới.

.EXAMPLE
    # Chạy từ thư mục gốc repo, sẽ hỏi mật khẩu MySQL
    powershell -ExecutionPolicy Bypass -File database\dev\create-dev-db.ps1

.EXAMPLE
    # Xóa và tạo lại từ đầu (MẤT TOÀN BỘ dữ liệu trong tigernet_dev)
    powershell -ExecutionPolicy Bypass -File database\dev\create-dev-db.ps1 -Reset

.EXAMPLE
    # Chỉ sinh file SQL để mở bằng MySQL Workbench, không chạy mysql.exe
    powershell -ExecutionPolicy Bypass -File database\dev\create-dev-db.ps1 -OutFile tigernet_dev.sql
#>
param(
    [string]$DbName   = "tigernet_dev",
    [string]$User     = "root",
    [string]$DbHost   = "localhost",
    [int]   $Port     = 3306,
    [switch]$Reset,
    [string]$OutFile,
    [string]$MysqlExe
)

$ErrorActionPreference = "Stop"

if ($DbName -notmatch '^[A-Za-z0-9_]+$') { throw "Tên database không hợp lệ: $DbName" }
if ($DbName -eq "tigernet")              { throw "Script này dành cho database dev, không chạy lên database chính 'tigernet'." }

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$source   = Join-Path $repoRoot "docs\Database_Script_v7.sql"
if (-not (Test-Path $source)) { throw "Không tìm thấy $source" }

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$sql = [System.IO.File]::ReadAllText($source, [System.Text.Encoding]::UTF8)

# Đổi tên database ở các dòng DROP/CREATE DATABASE và USE
$sql = [regex]::Replace($sql,
    '(?m)^(\s*(?:--\s*)?(?:DROP DATABASE IF EXISTS|CREATE DATABASE IF NOT EXISTS|USE))\s+tigernet\b',
    "`$1 $DbName")

if ($sql -notmatch "USE $DbName;") { throw "Không đổi được tên database trong script, kiểm tra lại docs/Database_Script_v7.sql." }

$header = "-- Sinh tự động từ docs/Database_Script_v7.sql bởi database/dev/create-dev-db.ps1`r`n"
if ($Reset) { $header += "DROP DATABASE IF EXISTS $DbName;`r`n" }
$sql = $header + $sql + "`r`nSET FOREIGN_KEY_CHECKS = 1;`r`n"

if ($OutFile) {
    $outPath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutFile))
    [System.IO.File]::WriteAllText($outPath, $sql, $utf8NoBom)
    Write-Host "Đã ghi $outPath. Mở bằng MySQL Workbench và chạy toàn bộ (Ctrl+Shift+Enter)."
    exit 0
}

# Tìm mysql.exe: tham số -MysqlExe, PATH, rồi thư mục cài mặc định của MySQL Server 8.x
if (-not $MysqlExe) {
    $cmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($cmd) { $MysqlExe = $cmd.Source }
}
if (-not $MysqlExe) {
    $MysqlExe = Get-ChildItem "C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $MysqlExe) {
    throw "Không tìm thấy mysql.exe. Thêm thư mục bin của MySQL vào PATH, truyền -MysqlExe, hoặc dùng -OutFile rồi chạy bằng Workbench."
}

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) "$DbName-init-$([guid]::NewGuid().ToString('N')).sql"
[System.IO.File]::WriteAllText($tmp, $sql, $utf8NoBom)

try {
    Write-Host "Tạo database $DbName trên ${DbHost}:$Port bằng $MysqlExe (nhập mật khẩu MySQL của $User)..."
    $proc = Start-Process -FilePath $MysqlExe -NoNewWindow -Wait -PassThru `
        -RedirectStandardInput $tmp `
        -ArgumentList "--user=$User", "--host=$DbHost", "--port=$Port", "--default-character-set=utf8mb4", "-p"
    if ($proc.ExitCode -ne 0) {
        throw "mysql trả về lỗi (mã $($proc.ExitCode)). Nếu database $DbName đã có bảng, chạy lại với -Reset để tạo lại từ đầu."
    }
    Write-Host "Xong. Đặt DB_NAME=$DbName trong backend/.env để backend dùng database này."
}
finally {
    Remove-Item $tmp -ErrorAction SilentlyContinue
}
