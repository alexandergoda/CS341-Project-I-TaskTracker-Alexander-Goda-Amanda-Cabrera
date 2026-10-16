# Project I Implementation Status

## Completed milestones

- Week 1 Requirements Analysis
- Week 1 SQLite/JDBC rapid prototype
- Prototype visual verification: connection, schema creation, insert, read-back
- Prototype JUnit verification: 2/2 passed, 0 errors, 0 failures
- Week 2 MVC architecture
- Week 2 database schema
- Week 2 UML class diagrams
- Week 2 unit and acceptance-test outlines

## Current implementation baseline

The shared final-project repository now implements the production model and database layer before the GUI:

- `tasktracker.model.User`
- `tasktracker.model.Task`
- `tasktracker.model.Priority`
- `tasktracker.data.DatabaseManager`
- `tasktracker.data.UserDAO`
- `tasktracker.data.TaskDAO`

The next implementation step is the controller layer, followed by Swing views.
