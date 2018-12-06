@ECHO OFF
CD..
SET ROOT=%CD%

REM --- SET THE PATH TO JAVA.EXE ---

SET JAVA="%ROOT%\jre\bin\java.exe"

REM --- START MOLECULER ---

%JAVA% -Xmx900m -server -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Djava.net.preferIPv4Stack=true "-Djava.library.path=%ROOT%\bin" -Djava.util.logging.config.file="%ROOT%\cfg\logger\with-console.properties" -classpath "%ROOT%\cfg;%ROOT%\lib\*" -Djava.io.tmpdir="%ROOT%\tmp" services.moleculer.config.MoleculerRunner /moleculer.config.xml

pause