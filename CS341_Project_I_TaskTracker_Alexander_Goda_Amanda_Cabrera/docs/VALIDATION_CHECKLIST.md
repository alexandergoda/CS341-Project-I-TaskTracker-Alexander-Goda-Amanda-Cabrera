# Model and Database Validation Checklist

After adding `lib/sqlite-jdbc-3.53.4.0.jar` in Eclipse:

1. Run `tasktracker.Main`.
   - Expect database initialization success.
   - Confirm `tasks.db` is created locally.
2. Run `DatabaseManagerTest`.
   - tables are created
   - foreign keys are enabled
3. Run `UserDAOTest`.
   - create/find user
   - duplicate username rejection
   - valid and invalid authentication query behavior
   - list users
4. Run `TaskDAOTest`.
   - create/read task
   - due date and priority persistence
   - relevant-task visibility query
   - update
   - completion
   - assignment/unassignment
   - deletion
5. Expected total in the current test package: **13 tests**.
6. Record any failure before proceeding to controller/UI integration.
