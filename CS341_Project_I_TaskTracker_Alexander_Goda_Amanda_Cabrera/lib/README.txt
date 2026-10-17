SQLite JDBC dependency
======================

Copy the plain Xerial SQLite JDBC driver into this folder with this exact name:

sqlite-jdbc-3.53.4.0.jar

The Eclipse .classpath file already references that relative path so both team members can use the same project structure without machine-specific paths.

Official artifact coordinates:
org.xerial:sqlite-jdbc:3.53.4.0

The JDBC JAR is a third-party dependency and is excluded from version control by /lib/*.jar in .gitignore.
