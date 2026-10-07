@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=C:\Program Files\Java\latest\jdk-25"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
echo ========================================================
call "E:\apache-maven-3.10.0\bin\mvn.cmd" tomcat7:run
pause
