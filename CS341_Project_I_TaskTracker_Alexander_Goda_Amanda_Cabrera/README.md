# CS341 Project I - Task Tracker

**Team:** Alexander Goda and Amanda Cabrera  
**Instructor:** Professor Trish Cornez  
**Course:** CS341-01 Software Engineering  
**Current implementation stage:** Week 3 foundation, model + SQLite/JDBC persistence before controllers/GUI

## Project purpose

Task Tracker is a standalone collaborative to-do list desktop application. Registered local users will be able to log in, create/edit/delete/complete tasks, assign tasks to registered users, and view tasks they created or were assigned. The selected extra feature is task due dates and priority levels.

## Current status

Completed in this repository:

- Eclipse/Java 11 project structure
- Model classes: `User`, `Task`, `Priority`
- SQLite schema initialization
- `UserDAO` persistence/authentication queries
- `TaskDAO` create/read/update/delete/assignment/completion/relevant-task queries
- SQL table design in `sql/schema.sql`
- JUnit 5 tests for the database manager, user DAO, and task DAO
- Controller and view package boundaries reserved for the next implementation step

Not implemented yet by design:

- `AuthController`
- `TaskController`
- Swing login/registration/dashboard/task-editor views
- final JavaDoc HTML generation
- final acceptance-test report and presentation/demo

The project is being built bottom-up so the model and persistence layer can be tested before the GUI is connected.

## Eclipse setup

1. Import this folder with **File > Import > General > Existing Projects into Workspace**.
2. Download the plain Xerial driver `sqlite-jdbc-3.53.4.0.jar`.
3. Copy it into the project's `lib` folder. The `.classpath` already points to `lib/sqlite-jdbc-3.53.4.0.jar`.
4. Refresh the project in Eclipse.
5. Verify **JRE System Library [JavaSE-11]** and **JUnit 5** are present.
6. Run `tasktracker.Main` as a Java Application.
7. Run the `tester` package as a JUnit Test.

Generated `.db` files and the local JDBC JAR are intentionally ignored by Git.

## Shared Git workflow

Both team members should make regular, meaningful commits. Pull before beginning work, keep commits focused, and avoid committing generated database files. See `TEAM_WORKFLOW.md`.
