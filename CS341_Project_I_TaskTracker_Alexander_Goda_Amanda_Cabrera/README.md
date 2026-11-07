# CS341 Project I - Task Tracker

**Team:** Alexander Goda and Amanda Cabrera  
**Instructor:** Professor Trish Cornez  
**Course:** CS341-01 Software Engineering

## Overview

Task Tracker is a standalone Java Swing application with local accounts and SQLite persistence. Users can manage tasks they created or that were assigned to them. Tasks support an optional due date, optional assignment and LOW, MEDIUM or HIGH priority.

The application uses one window with startup, sign-in/registration, a task dashboard, an inline task editor and a signed-in password-change form. Six scenic appearances share the same layout. Auto follows the local clock; manual controls select a scene or Midday/Night. The creator credit appears on all screens.

## Requirements

- JDK 11 or newer. The supplied Eclipse settings target Java 11; the recorded build and tests used Java 11.
- Eclipse with Java development and JUnit 5 support.
- Xerial SQLite JDBC 3.53.4.0 at `lib/sqlite-jdbc-3.53.4.0.jar`.
- The supplied `assets` directory on the runtime classpath.

This is a plain Eclipse Java project, not a Maven project. Source, test, resource and driver paths are relative to the project root. [lib/README.txt](lib/README.txt) gives the exact driver download, filename and checksum. The driver is available in the local working copy but is excluded from Git; a source-only checkout needs that dependency supplied separately.

## Import and run in Eclipse

1. Use **File > Import > General > Existing Projects into Workspace**. Select the project directory containing `.project` and `.classpath`, then finish the import. Preserve the supplied `.settings` directory too. Import the whole project, rather than importing only `src` or creating a second Java project around it. If the workspace already contains a project with the same name, use a separate workspace so the existing project is preserved.
2. Follow [lib/README.txt](lib/README.txt) if the driver is missing. Refresh the project after adding it.
3. Under **Project > Properties > Java Build Path**, confirm `src` and `test` are source folders, `JavaSE-11` and `JUnit 5` are present, and the JDBC JAR and `assets` entries resolve. Keep `test`: it contains the repeatable JUnit and standalone checks.
4. Under **Java Compiler**, confirm project-specific compliance is **11**. Selecting JavaSE-11 alone does not replace the compiler-level check. Under **Resource**, confirm the text-file encoding is **UTF-8**. Use a Java 11 JDK for the supplied configuration; check **Installed JREs/Execution Environments** if JavaSE-11 is unresolved.
5. Clean-build the project with **Project > Clean**. In **Run > Run Configurations > Java Application**, select `tasktracker.Main`, use Java 11, and set **Arguments > Working directory** to this project directory. Run the application.
6. On a fresh local database, choose **Create account**, register and then sign in. There are no required shared demo credentials. The application creates `tasks.db` and the required tables automatically.

The database path is relative to the launch working directory. Use the same working directory across launches to return to the same accounts and tasks. If a launch appears to have lost local data, check its working directory before creating or replacing a database.

Local databases, SQLite sidecars and generated class files are excluded from Git. Preserve a working database before any replacement or reset; do not delete existing data merely to demonstrate first run. A separate disposable copy or fresh working directory can demonstrate initialization without altering the team's data.

## Application behavior

- Registration rejects blank usernames, empty passwords and duplicate usernames. Login and logout maintain the current local session.
- A signed-in user can change their password from the account menu by supplying the current password and confirming the new password.
- The dashboard includes tasks created by the current user **or** assigned to that user.
- Only the creator can edit, delete or change assignment. The creator or assignee can mark the task done or reopen it.
- Completed tasks remain stored and are hidden by default. **Show completed** reveals them; **Hide completed** hides them again. **Reopen** returns a task to active work.
- The selected task's focus panel permanently shows its creator, assignee, named priority, due date and notes. Task metadata stays visible without hover.
- Due-date entry uses U.S. month/day/year, for example `2/29/2028`, and displays as `02/29/2028` in the editor. Blank means no due date. SQLite stores an ISO date string. Invalid dates are rejected.
- Incomplete tasks come before completed tasks. Within those groups, due dates sort earliest first with undated tasks last, then priority HIGH to LOW, then task ID. Group labels include Overdue, Today, Tomorrow, Upcoming, No date and Completed.
- New and edited tasks use the inline form. Navigation away from a changed draft offers **Save and continue**, **Discard changes** or **Keep editing**. Keep editing preserves the draft, selection and completed-task visibility. If Save and continue fails validation, correct the field and retry the pending action.

## Architecture

Startup follows `Main` to `TaskTrackerUI.launch`, then `ApplicationWindow` and its `AppWorkspace` coordinator.

| Area | Responsibility |
| --- | --- |
| `model` | `User`, `Task` and `Priority` data types. |
| `controller` | `AuthController` manages accounts, session and password change. `TaskController` validates task operations and permissions. |
| `data` | `DatabaseManager` initializes the database and connections. `UserDAO` and `TaskDAO` use parameterized JDBC operations. |
| `view` | Application window and workspace coordination. |
| `view/screens` | `StartupSurface`, `AuthSurface`, `DashboardSurface`, `InlineEditorSurface` and `PasswordChangeSurface`. Registration is part of `AuthSurface`. |
| `view/components` | Buttons, account/scene menus, date picker, assignment selector, priority picker and scrolling controls. |
| `view/theme`, `view/animation`, `view/presentation` | Scene resources/lighting, transitions, credit/date display and task ordering. |

The schema has users and tasks, with a required creator and optional assignee foreign key. [sql/schema.sql](sql/schema.sql) is the SQL reference; `DatabaseManager` creates matching tables automatically. Passwords are stored as unsalted SHA-256 hashes. `User` model objects do not contain password data. Password-specific salted hashing and database encryption are outside the current implementation.

## Artwork

Keep `assets` as the classpath resource root. The application requires these seven PNGs in `assets/scenes`:

- `background-dawn.png`
- `background-sunrise.png`
- `background-midday.png`
- `background-golden-hour.png`
- `background-dusk.png`
- `background-night.png`
- `foreground-dusk.png`

The renderer generates matching foreground lighting from the master foreground. The six backgrounds also provide scene-lighting references. Images load through classpath resources. The supplied project runs through Eclipse with the assets directory on its classpath.

## Testing

Run the `tester` package under the `test` source folder as **JUnit Test**. The inventory is **33 JUnit test methods across six classes**:

| Class | Test methods |
| --- | ---: |
| `DatabaseManagerTest` | 2 |
| `UserDAOTest` | 5 |
| `TaskDAOTest` | 6 |
| `AuthControllerTest` | 6 |
| `TaskControllerTest` | 9 |
| `PasswordChangeTest` | 5 |

The suite covers initialization, application foreign-key enforcement, accounts/session rules, password changes, task CRUD, permissions, assignment, optional dates/priority, completion and reopening. It uses isolated temporary SQLite databases, so DAO/controller checks include real database integration.

The `test/tasktracker` packages also contain **25 separate standalone check programs**, each with a `main` method. Run each as a Java Application with **VM argument** `-Djava.awt.headless=true` and the project's normal build path. The testing report lists every fully qualified check class. These cover component interactions, task presentation, unsaved-draft decisions, resource loading, rendering, transitions and callback cleanup. Count their results separately from JUnit.

Verification on **November 7, 2026** passed **33/33 JUnit tests** and **25/25 standalone programs**. The imported Eclipse project had **0 errors and 0 warnings**. Independent Java 11 compilation with `--release 11 -encoding UTF-8 -Xlint:all` and production JavaDoc with `-Xdoclint:all` also reported **0 errors and 0 warnings**.

We completed a manual walkthrough on macOS on November 7 and found no issues. The [testing report](docs/CS341_Project_I_Testing_Report_Alexander_Goda_Amanda_Cabrera.docx) documents the test procedures, results and coverage.

## JavaDoc API reference

Open [docs/JavaDoc/index.html](docs/JavaDoc/index.html) for the included generated API documentation. It describes the production application packages under `src`. JUnit tests and standalone checks remain available as source under `test`; they are not part of the application API export.

Regenerate the API documentation after changing application source or its documentation comments. In Eclipse, select the project and use **Project > Generate Javadoc**. Select the application packages under `src`, use the **JDK 11 `javadoc` executable**, and set the destination to this project's `docs/JavaDoc` directory. Keep the resolved JDBC classpath and UTF-8 source encoding. Use `-Xdoclint:all` in the additional JavaDoc options for the strict documentation check.

Replace only the generated `docs/JavaDoc` tree when refreshing this export, preserving the generator's legal/license files. Keep the source, tests and other project documents intact. Open `index.html` and follow its package/class links to check the refreshed export before using it.

## Documentation and project folders

| Location | Contents |
| --- | --- |
| `src` | Application Java source. |
| `test` | JUnit tests and standalone check source. |
| `assets` | Required scene artwork. |
| `lib` | JDBC dependency and setup instructions. |
| `sql` | Schema reference. |
| `docs/Requirements` | Requirements analysis, use cases and SRS material. |
| `docs/Design` | Design package with embedded architecture, schema and UML diagrams. |
| `docs/JavaDoc` | Generated production API documentation, including `index.html`. |
| `docs` | Testing report. |

- [Requirements analysis](docs/Requirements/CS341_Project_I_Requirements_Analysis_Alexander_Goda_Amanda_Cabrera.docx)
- [Design package](docs/Design/CS341_Project_I_Design_Package_Alexander_Goda_Amanda_Cabrera.docx)
- [Testing report](docs/CS341_Project_I_Testing_Report_Alexander_Goda_Amanda_Cabrera.docx)
- [JavaDoc API reference](docs/JavaDoc/index.html)
