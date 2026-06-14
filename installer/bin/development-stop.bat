@ECHO OFF
CD..
SET ROOT=%CD%

REM --- SET THE PATH TO JAVA.EXE ---

SET JAVA="%ROOT%\jre\bin\java.exe"

REM --- STOP MOLECULER ---

%JAVA% -Xmx900m -server -Djava.net.preferIPv4Stack=true -Dlogging.config="%ROOT%\cfg\logging-development.properties" -classpath "%ROOT%\cfg;%ROOT%\lib\*" -Djava.io.tmpdir="%ROOT%\tmp" services.moleculer.config.MoleculerRunner stop