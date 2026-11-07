SQLite JDBC dependency
======================

Task Tracker uses the plain Xerial SQLite JDBC driver, version 3.53.4.0.
The project does not use Maven to download dependencies automatically.

Official release:
https://github.com/xerial/sqlite-jdbc/releases/tag/3.53.4.0

Download the plain sqlite-jdbc-3.53.4.0.jar release asset. Keep the JAR
unextracted and place it in this lib folder with this exact filename:

sqlite-jdbc-3.53.4.0.jar

Official artifact coordinates:
org.xerial:sqlite-jdbc:3.53.4.0

The Eclipse .classpath file already references that relative path. Refresh
the project after placing the JAR, then confirm the library resolves under
Project > Properties > Java Build Path. Both team members can use the same
project layout without machine-specific paths.

Verified local artifact
-----------------------
Size: 12,002,370 bytes
SHA-256:
bcb1f51e36f940867e83342f9efbf5968ac44a6bef4d397bb4af7b17b45cd2fb

To compare a downloaded file with the verified local artifact:

macOS, from the project root:
shasum -a 256 lib/sqlite-jdbc-3.53.4.0.jar

Windows PowerShell, from the project root:
Get-FileHash lib/sqlite-jdbc-3.53.4.0.jar -Algorithm SHA256

The JDBC JAR is a third-party dependency excluded from version control by
/lib/*.jar in .gitignore. A source-only checkout therefore needs the JAR
provided separately.
Do not substitute a different driver version without rechecking the build
and tests.
