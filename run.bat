@REM @echo off
@REM title IRMS Web Server - Tomcat
@REM set "JAVA_HOME=D:\Program Files\Java\jdk-25.0.4"
@REM set "PATH=%JAVA_HOME%\bin;%PATH%"
@REM echo ========================================================
@REM echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
@REM echo ========================================================
@REM "D:\bluesstack\apache-maven-3.10.0\bin\mvn.cmd" tomcat7:run
@REM pause
@echo off
title IRMS Web Server - Tomcat
set "JAVA_HOME=C:\Program Files\Java\jdk-27"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo ========================================================
echo   DANG KHOI DONG HE THONG IRMS TAI HTTP://LOCALHOST:8080
echo ========================================================
"E:\maven\apache-maven-3.10.0\bin\mvn.cmd" tomcat7:run
pause
