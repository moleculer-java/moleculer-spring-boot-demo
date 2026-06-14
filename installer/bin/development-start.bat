@ECHO OFF
CD..
SET ROOT=%CD%

REM --- SET THE PATH TO JAVA.EXE ---

SET JAVA="%ROOT%\jre\bin\java.exe"

REM --- START MOLECULER ---

%JAVA% -Xmx900m -server -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=4000 -Djava.net.preferIPv4Stack=true -classpath "%ROOT%\cfg;%ROOT%\lib\*" -Djava.io.tmpdir="%ROOT%\tmp" -Dlogging.config="%ROOT%\cfg\logging-development.properties" -Dspring.profiles.active=development services.moleculer.config.MoleculerRunner my.application.MoleculerApplication

pause