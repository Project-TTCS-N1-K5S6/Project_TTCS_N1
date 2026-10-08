@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
echo ========================================================
call "E:\maven\apache-maven-3.10.0\bin\mvn.cmd" -o tomcat7:run
pause
