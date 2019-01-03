@ECHO OFF

REM --- INSTALL MOLECULER AS WINDOWS SERVICE ---

Tomcat7.exe //IS/MoleculerJava --Startup="auto" --DisplayName="Moleculer" --Description="Moleculer Java Service" \
