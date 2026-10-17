# Project I Implementation Status

## Completed milestones

- Week 1 Requirements Analysis and SQLite/JDBC rapid prototype
- Recorded prototype verification: 2/2 JUnit tests, 0 errors, 0 failures
- Week 2 MVC architecture, production schema, UML, and test outlines
- Production model and persistence: User, Task, Priority, DatabaseManager, UserDAO, TaskDAO
- Controllers: AuthController and TaskController
- Swing views: LoginView, RegisterView, TaskDashboardView, TaskEditorView
- Registration/login/logout; task creation/editing/deletion; assignment/unassignment
- Creator-only editing/deletion/assignment; creator-or-assignee completion changes
- Relevant-task visibility; optional due dates; LOW/MEDIUM/HIGH priority with MEDIUM default
- SQLite persistence and automatic schema creation

## Recorded verification

The October 16, 2026 testing report records 25/25 automated tests passing, 0 errors, 0 failures, and nine passing manual acceptance checks, including full application restart persistence. Eclipse Problems showed 0 errors and 0 warnings in that recorded session. These describe the original session; the post-fix JUnit rerun and manual check M10 are recorded below.

The five test classes contain 2 database, 5 user DAO, 6 task DAO, 6 authentication, and 6 task-controller tests. Week 2 test outlines remain planning records. Default-priority and blank-title planned unit tests were not added to this suite; blank-title rejection has manual evidence in M07.

## Final documentation reconciliation

Requirements refinements now describe registration confirmation, optional/self assignment, and completion reopening. MVC/UML diagrams match implemented symbols and return types. The schema matches both embedded initialization SQL and sql/schema.sql. The Week 1 and Week 2 history remains in its existing folders.

## Verified logout correction

TaskDashboardView tracks each task editor it opens, removes closed editors from its list, and disposes all remaining editors when the dashboard is disposed. Logout disposes the dashboard and its editors before clearing the authenticated session and invoking the login callback.

The post-fix Eclipse JUnit rerun on October 16, 2026 completed 25/25 tests with 0 errors and 0 failures in 0.413 seconds, as shown in the 11:33 PM screenshot. The existing suite still contains 25 methods; no new automated GUI test was added.

Manual regression check M10 passed: the user confirmed that both New Task and Edit Task windows closed automatically when Logout was clicked. The subsequent screenshot shows Amanda's dashboard without the old editors. This is manual GUI evidence, separate from the JUnit rerun.

## Remaining final-delivery work

- Perform the final presentation/demo and submission preparation.
- Rerun relevant checks after any implementation change and record only observed results.


## Latest final audit

Final audit addendum dated October 16, 2026. This addendum supersedes current-suite counts and coverage-limit statements above while preserving historical results and Week 1/Week 2 plans. The expanded Eclipse JUnit 5 suite passed 28/28 tests with 0 errors and 0 failures in 0.444 seconds. Class counts are DatabaseManagerTest 2, UserDAOTest 5, TaskDAOTest 6, AuthControllerTest 6, and TaskControllerTest 9. Three new controller tests cover rejection of every public task operation while logged out, self-assignment without duplicate dashboard results with MEDIUM default and optional date, completion and reopening followed by unassignment, and blank-title create/edit rejection with saved data unchanged. These are automated controller/SQLite checks, not GUI passes. Editor-window cleanup remains covered by the previously recorded manual M10 only. The fresh Problems tab reported 0 items in its current configuration. Production sources compiled on JDK 11.0.30 with --release 11 and -Xlint:all without diagnostics. Eclipse launched Main and reported database initialization success, but native automation could not bind to the Java window; no new manual GUI pass is claimed. See docs/FINAL_AUDIT.md and docs/FINAL_SUBMISSION_CHECKLIST.md for delivery status and limitations.
