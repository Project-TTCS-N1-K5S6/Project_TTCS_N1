@echo off
title IRMS Web Server - Tomcat
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java was not found at "%JAVA_HOME%".
    echo Set JAVA_HOME to the installed JDK directory and try again.
    goto :failed
)

if not defined MAVEN_HOME set "MAVEN_HOME=C:\Users\ADMIN\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo ERROR: Maven was not found at "%MAVEN_HOME%".
    echo Set MAVEN_HOME to the installed Maven directory and try again.
    goto :failed
)

if not exist ".env" (
    echo ERROR: .env was not found in the project directory.
    goto :failed
)

set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do if not "%%A"=="" set "%%A=%%B"

if defined DB_HOST if defined DB_PORT if defined DB_NAME set "DB_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
if defined DB_POOL_MIN set "DB_POOL_MINIDLE=%DB_POOL_MIN%"
if defined DB_POOL_MAX set "DB_POOL_MAXIMUMPOOLSIZE=%DB_POOL_MAX%"
if defined MAX_FAILED_LOGIN_ATTEMPTS set "SECURITY_MAXFAILEDATTEMPTS=%MAX_FAILED_LOGIN_ATTEMPTS%"
if defined ACCOUNT_LOCK_MINUTES set "SECURITY_LOCKOUTDURATIONMINUTES=%ACCOUNT_LOCK_MINUTES%"
if defined SESSION_TIMEOUT_MINUTES set "SECURITY_SESSIONTIMEOUTMINUTES=%SESSION_TIMEOUT_MINUTES%"
if defined JWT_ACCESS_SECRET set "SECURITY_JWTSECRET=%JWT_ACCESS_SECRET%"
if defined APP_URL set "APP_BASEURL=%APP_URL%"

if /I "%~1"=="local" (
    set "DB_URL=jdbc:mysql://127.0.0.1:3306/irms_local_dev_20261008?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
    set "DB_USER=root"
    set "DB_PASSWORD="
    echo Using isolated local IRMS database; shared team database is not selected.
) else (
    echo Using database configuration from .env.
)

echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:%PORT%
echo ========================================================
call "%MAVEN_HOME%\bin\mvn.cmd" tomcat7:run
if errorlevel 1 goto :failed
exit /b 0

:failed
pause
endlocal
exit /b 1