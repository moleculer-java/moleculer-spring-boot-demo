@ECHO OFF
CD..
SET ROOT=%CD%

REM --- SET THE PATH TO JAVA.EXE ---

SET JAVA="%ROOT%\jre\bin\java.exe"

REM --- START MOLECULER ---

%JAVA% -Xmx900m -server -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Djava.net.preferIPv4Stack=true "-Djava.library.path=%ROOT%\bin" -classpath "%ROOT%\cfg;%ROOT%\lib\*" -Djava.io.tmpdir="%ROOT%\tmp" -Dlogging.config="%ROOT%\cfg\logging-development.properties" -Dspring.profiles.active=development services.moleculer.config.MoleculerRunner my.application.MoleculerApplication

pause