# CS341 Project I - Task Tracker

**Team:** Alexander Goda and Amanda Cabrera  
**Instructor:** Professor Trish Cornez  
**Course:** CS341-01 Software Engineering

## Project Overview

Task Tracker is a standalone collaborative to-do list desktop application developed in Java.

The application is designed to allow registered users to manage tasks through individual local accounts. Users will be able to register, log in and log out, create tasks, edit tasks, delete tasks, mark tasks as complete, assign tasks to registered users, and view tasks they created or that were assigned to them.

The project uses task due dates and priority levels as its additional task-management feature.

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
- SQLite
- JDBC
- Xerial SQLite JDBC driver
- JUnit 5
- Git and GitHub

## Architecture

Task Tracker follows the Model-View-Controller architectural pattern.

### Model and Persistence

The model and persistence layers manage the application's data and database operations.

Current classes include:

- `User`
- `Task`
- `Priority`
- `DatabaseManager`
- `UserDAO`
- `TaskDAO`

SQLite is used for local persistent storage through JDBC.

### Controller

The controller layer coordinates actions between the user interface and the model/persistence layer.

Planned controller classes include:

- `AuthController`
- `TaskController`

### View

The view layer will provide the Swing graphical user interface for:

- registration
- login
- task dashboard
- task creation and editing
- task assignment
- task completion and deletion

## Database

The application uses a local SQLite database.

The current database design includes:

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

The database tables are created automatically when the application initializes.

## Current Implementation Status

The following components have been implemented:

- Java 11 Eclipse project structure
- `User`, `Task`, and `Priority` model classes
- SQLite database initialization
- user persistence and authentication queries
- task creation, retrieval, editing, deletion, assignment, and completion operations
- retrieval of tasks created by or assigned to a user
- database schema
- JUnit 5 tests for the database and persistence layer

The controller and graphical user interface layers are currently under development.

## Testing

JUnit 5 is used for automated testing.

The current automated test suite includes tests for:

- database initialization
- foreign-key behavior
- user registration
- duplicate usernames
- user lookup
- authentication
- task creation
- task retrieval
- task editing
- task assignment and unassignment
- task completion
- task deletion
- persistence of task due dates and priorities

Manual and visual testing will also be performed as the controller and GUI layers are integrated.

## Project Structure

```text
src/
└── tasktracker/
    ├── Main.java
    ├── model/
    ├── data/
    ├── controller/
    └── view/

test/
└── tester/

sql/
└── schema.sql

docs/