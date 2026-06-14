@ECHO OFF

REM --- INSTALL MOLECULER AS WINDOWS SERVICE ---

prunsrv.exe //IS//MoleculerJava --Startup="auto" --DisplayName="Moleculer" --Description="Moleculer Java Service"
