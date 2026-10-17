# CS341 Project I - Task Tracker

**Team:** Alexander Goda and Amanda Cabrera  
**Instructor:** Professor Trish Cornez  
**Course:** CS341-01 Software Engineering

## Project Overview

Task Tracker is a standalone collaborative to-do list desktop application developed in Java.

The application allows registered users to manage tasks through individual local accounts. Users can register, log in and log out, create tasks, edit tasks, delete tasks, mark tasks as complete, assign tasks to registered users, and view tasks they created or that were assigned to them.

Tasks support optional due dates and LOW, MEDIUM, or HIGH priorities.

## Project Objectives

The primary objectives of Task Tracker are to:

- provide a simple desktop interface for managing personal and assigned tasks
- support multiple registered local users
- persist users and tasks between application sessions
- organize the application using Model-View-Controller architecture
- apply object-oriented design principles and modular development
- verify important application behavior using JUnit 5 and manual testing
- use Git and GitHub for collaborative version control

## Technology

- Java 11 or newer
- Eclipse IDE
- Java Swing
- SQLite
- JDBC
- Xerial SQLite JDBC driver
- JUnit 5
- Git and GitHub

## Architecture

Task Tracker follows the Model-View-Controller architectural pattern.

### Model and Persistence

The model and persistence layers manage application data and database operations.

Classes include:

- `User`
- `Task`
- `Priority`
- `DatabaseManager`
- `UserDAO`
- `TaskDAO`

SQLite is used for local persistent storage through JDBC.

### Controller

The controller layer coordinates actions between the graphical user interface and the model/persistence layer.

Controller classes include:

- `AuthController`
- `TaskController`

`AuthController` manages registration, login, logout, and authenticated-user state.

`TaskController` manages task operations and enforces permissions for task editing, deletion, assignment, and completion.

### View

The view layer provides the Swing graphical user interface.

View classes include:

- `LoginView`
- `RegisterView`
- `TaskDashboardView`
- `TaskEditorView`

The interface supports:

- registration
- login and logout
- task dashboard viewing
- task creation
- task editing
- task assignment
- task completion and reopening
- task deletion
- due-date entry
- priority selection

## Database

The application uses a local SQLite database.

The database contains two primary tables.

### Users

Stores registered user account information.

### Tasks

Stores task information including:

- task title
- description
- creator
- assigned user
- completion status
- due date
- priority level

The database and required tables are created automatically when the application initializes.

## Testing

JUnit 5 is used for automated testing.

The automated suite contains 28 test methods across five classes. The recorded results are:

- **28/28 runs**
- **0 errors**
- **0 failures**

The automated tests cover:

- database initialization
- foreign-key behavior
- user registration
- duplicate usernames
- user lookup
- authentication
- login and logout state
- task creation
- task retrieval
- task editing
- task assignment and unassignment
- task completion
- task deletion
- persistence of task due dates and priorities
- creator edit permissions
- creator delete permissions
- prevention of unauthorized editing
- prevention of unauthorized deletion
- assignee completion permissions
- prevention of completion by unrelated users
- rejection of task operations while logged out
- self-assignment without duplicate dashboard results
- default MEDIUM priority and optional due dates
- completion reopening and unassignment
- blank-title rejection without overwriting saved data

Manual and visual testing has also been performed through the Swing interface.

Verified GUI workflows include:

- registering local user accounts
- logging in and logging out
- creating tasks
- editing existing tasks
- assigning tasks to another registered user
- viewing assigned tasks from the assignee account
- marking assigned tasks as complete
- confirming completion changes persist between accounts
- deleting tasks
- confirming deleted tasks no longer appear
- verifying task information persists through SQLite between login sessions

## Project Structure

```text
src/
└── tasktracker/
    ├── Main.java
    ├── model/
    │   ├── User.java
    │   ├── Task.java
    │   └── Priority.java
    ├── data/
    │   ├── DatabaseManager.java
    │   ├── UserDAO.java
    │   └── TaskDAO.java
    ├── controller/
    │   ├── AuthController.java
    │   └── TaskController.java
    └── view/
        ├── LoginView.java
        ├── RegisterView.java
        ├── TaskDashboardView.java
        └── TaskEditorView.java

test/
└── tester/
    ├── DatabaseManagerTest.java
    ├── UserDAOTest.java
    ├── TaskDAOTest.java
    ├── AuthControllerTest.java
    └── TaskControllerTest.java

sql/
└── schema.sql

docs/
├── Requirements/
├── Design/
└── CS341_Project_I_Testing_Report_Alexander_Goda_Amanda_Cabrera.docx
```

## Implementation details

- The Complete control toggles Open/Complete, including reopening completed tasks.
- The editor supports self-assignment and Unassigned; one dashboard combines created and assigned tasks, ordered by completion state and task ID.
- AuthController hashes passwords using unsalted SHA-256. User objects contain no password data.
- `tasks.db` is relative to the working directory. DatabaseManager executes embedded schema SQL; `sql/schema.sql` is a matching reference.
- Navigation and dashboard refresh use Main and Runnable callbacks; views interpret controller results and exceptions.

## Session lifecycle and test coverage

TaskDashboardView tracks open task editors and disposes them when the dashboard closes. Logout closes the dashboard and its editors before clearing the authenticated session and returning to login.

The testing report records manual acceptance checks M01–M09 and the manually verified M10 logout regression check. Editor cleanup is covered by manual verification; the 28 automated tests exercise controllers and SQLite persistence, not Swing GUI interaction.

## Running a clean checkout

1. Install JDK 11 or newer and use Eclipse with Java and JUnit 5 support.
2. Import this folder with File > Import > Existing Projects into Workspace. The Git repository contains this Eclipse project inside its named subdirectory.
3. Obtain the Xerial driver named in `lib/README.txt` and place it at `lib/sqlite-jdbc-3.53.4.0.jar`. The JAR is intentionally ignored by Git.
4. Refresh the project, confirm JavaSE-11 and JUnit 5 in the build path, and build. The classpath uses relative paths.
5. Run `src/tasktracker/Main.java` as a Java Application. Use this project directory as the working directory for a consistent local `tasks.db`.
6. Register a local account, close the registration window, then log in. The application creates its database and tables automatically.
7. Run the entire `test/tester` package as JUnit Test. The expected inventory is 28 tests.

## Project documentation

- Requirements and use cases: `docs/Requirements/`
- Design package and diagrams: `docs/Design/`
- Testing report: `docs/CS341_Project_I_Testing_Report_Alexander_Goda_Amanda_Cabrera.docx`
