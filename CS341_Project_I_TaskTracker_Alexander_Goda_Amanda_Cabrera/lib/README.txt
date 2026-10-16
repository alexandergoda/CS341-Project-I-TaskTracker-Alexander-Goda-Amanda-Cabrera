SQLite JDBC dependency
======================

Copy the plain Xerial SQLite JDBC driver into this folder with this exact name:

sqlite-jdbc-3.53.4.0.jar

The Eclipse .classpath file already references that relative path so both team members can use the same project structure without machine-specific paths.

Official artifact coordinates:
org.xerial:sqlite-jdbc:3.53.4.0

Do not commit the JAR unless the team deliberately decides to vendor the third-party dependency. The current .gitignore excludes /lib/*.jar.
