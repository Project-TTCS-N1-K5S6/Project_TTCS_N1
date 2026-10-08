@REM @echo off
@REM title IRMS Web Server - Tomcat
@REM setlocal
@REM cd /d "%~dp0"

@REM if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
@REM if not exist "%JAVA_HOME%\bin\java.exe" (
@REM     echo ERROR: Java was not found at "%JAVA_HOME%".
@REM     echo Set JAVA_HOME to the installed JDK directory and try again.
@REM     goto :failed
@REM )

@REM if not defined MAVEN_HOME set "MAVEN_HOME=C:\Users\ADMIN\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0"
@REM if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
@REM     echo ERROR: Maven was not found at "%MAVEN_HOME%".
@REM     echo Set MAVEN_HOME to the installed Maven directory and try again.
@REM     goto :failed
@REM )

@REM if not exist ".env" (
@REM     echo ERROR: .env was not found in the project directory.
@REM     goto :failed
@REM )

@REM set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
@REM for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do if not "%%A"=="" set "%%A=%%B"

@REM if defined DB_HOST if defined DB_PORT if defined DB_NAME set "DB_URL=jdbc:mysql://%DB_HOST%:%DB_PORT%/%DB_NAME%?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
@REM if defined DB_POOL_MIN set "DB_POOL_MINIDLE=%DB_POOL_MIN%"
@REM if defined DB_POOL_MAX set "DB_POOL_MAXIMUMPOOLSIZE=%DB_POOL_MAX%"
@REM if defined MAX_FAILED_LOGIN_ATTEMPTS set "SECURITY_MAXFAILEDATTEMPTS=%MAX_FAILED_LOGIN_ATTEMPTS%"
@REM if defined ACCOUNT_LOCK_MINUTES set "SECURITY_LOCKOUTDURATIONMINUTES=%ACCOUNT_LOCK_MINUTES%"
@REM if defined SESSION_TIMEOUT_MINUTES set "SECURITY_SESSIONTIMEOUTMINUTES=%SESSION_TIMEOUT_MINUTES%"
@REM if defined JWT_ACCESS_SECRET set "SECURITY_JWTSECRET=%JWT_ACCESS_SECRET%"
@REM if defined APP_URL set "APP_BASEURL=%APP_URL%"

@REM if /I "%~1"=="local" (
@REM     set "DB_URL=jdbc:mysql://127.0.0.1:3306/irms_local_dev_20261008?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
@REM     set "DB_USER=root"
@REM     set "DB_PASSWORD="
@REM     echo Using isolated local IRMS database; shared team database is not selected.
@REM ) else (
@REM     echo Using database configuration from .env.
@REM )

@REM echo ========================================================
@REM echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:%PORT%
@REM echo ========================================================
@REM call "%MAVEN_HOME%\bin\mvn.cmd" tomcat7:run
@REM if errorlevel 1 goto :failed
@REM exit /b 0

@REM :failed
@REM pause
@REM endlocal
@REM exit /b 1
@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
echo ========================================================
"E:\maven\apache-maven-3.10.0\bin\mvn.cmd" tomcat7:run
pause
