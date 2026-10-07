@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=D:\Program Files\Java\jdk-25.0.4"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
echo ========================================================
"D:\bluesstack\apache-maven-3.10.0\bin\mvn.cmd" tomcat7:run
pause
