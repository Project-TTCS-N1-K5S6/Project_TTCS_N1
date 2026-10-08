@echo off
chcp 65001 >nul
title IRMS Web Platform - Internal Recruitment System
cls
echo ==============================================================================
echo        HỆ THỐNG QUẢN LÝ TUYỂN DỤNG VÀ NHÂN SỰ NỘI BỘ (IRMS PLATFORM)
echo ==============================================================================
echo.
echo [*] Đang tìm kiếm môi trường Node.js...

set "NODE_CMD="

where node >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "NODE_CMD=node"
    goto found_node
)

if exist "C:\Program Files\Microsoft Visual Studio\18\Community\MSBuild\Microsoft\VisualStudio\NodeJs\node.exe" (
    set "NODE_CMD=C:\Program Files\Microsoft Visual Studio\18\Community\MSBuild\Microsoft\VisualStudio\NodeJs\node.exe"
    goto found_node
)

if exist "%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe" (
    set "NODE_CMD=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
    goto found_node
)

echo [!] Không tìm thấy Node.js! Vui lòng cài đặt Node.js hoặc kiểm tra đường dẫn.
pause
exit /b 1

:found_node
echo [*] Đã tìm thấy Node.js: "%NODE_CMD%"
echo.
echo ==============================================================================
echo  [*] Máy chủ đang khởi động tại: http://localhost:3000/
echo  [*] Tài khoản Quản trị viên:    admin@company.local   ^| Mật khẩu: Admin@123456
echo  [*] Tài khoản Trưởng phòng NS:  hr.manager@company.local ^| Mật khẩu: Admin@123456
echo  [*] Tài khoản Bị khóa thử:      locked.user@company.local ^| Mật khẩu: Admin@123456
echo ==============================================================================
echo.
echo [*] Đang mở trình duyệt web...
start http://localhost:3000/

echo.
echo [*] Nhấn Ctrl+C để dừng máy chủ.
echo.
"%NODE_CMD%" server.js
pause
