# Moleculer Java demo project for Spring Framework

The project demonstrates the framework of a functioning Moleculer-based application.
The application is launched and configured using the Spring Framework,
the configuration files are located in the "cfg" directory.

The project can be imported into Eclipse IDE.

The project also includes a "buildInstaller" Gradle command to create a **Windows Installer** from the project,
and it will install the finished application as a 64-bit **Windows Service**.

The Windows Service creates a Moleculer Node that can be connected to another **Java or NodeJS-based** Moleculer Node.
The connection parameters can be specified in the "cfg/moleculer.config.xml" file and and in the files of the "cfg/transporter" directory.