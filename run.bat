@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "target\tomcat\conf" mkdir "target\tomcat\conf"
if exist "src\main\tomcatconf" xcopy /Y /Q /E "src\main\tomcatconf\*" "target\tomcat\conf\" >nul 2>&1
if not exist "target\tomcat\conf\tomcat-users.xml" (
    echo ^<?xml version="1.0" encoding="UTF-8"?^> > "target\tomcat\conf\tomcat-users.xml"
    echo ^<tomcat-users^>^</tomcat-users^> >> "target\tomcat\conf\tomcat-users.xml"
)

echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8088
echo ========================================================
if exist "target\tomcat" if not exist "target\tomcat\conf\tomcat-users.xml" (
    echo [INFO] Phat hien cau hinh Tomcat loi, dang don dep target\tomcat...
    rd /s /q "target\tomcat" >nul 2>&1
)



if exist "E:\maven\apache-maven-3.10.0\bin\mvn.cmd" (
    call "E:\maven\apache-maven-3.10.0\bin\mvn.cmd" -o tomcat7:run
) else (
    call mvn.cmd -o tomcat7:run
)
pause
