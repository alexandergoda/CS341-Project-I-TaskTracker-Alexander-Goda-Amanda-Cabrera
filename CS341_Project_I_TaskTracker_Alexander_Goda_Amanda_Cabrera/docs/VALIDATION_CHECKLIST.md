# Final Implementation Validation Checklist

This checklist defines repeatable verification steps. The testing report records the original M01-M09 acceptance checks, the post-fix user-confirmed M10 logout check, and the screenshot-confirmed 25/25 JUnit rerun (0 errors, 0 failures; 0.413 seconds) on October 16, 2026.

## Environment and startup

1. Confirm Java 11 or newer, Eclipse JUnit 5, and lib/sqlite-jdbc-3.53.4.0.jar on the classpath.
2. Run tasktracker.Main. Expect initialization success and the Swing login window.
3. Confirm tasks.db is created in the working directory. DatabaseManager initializes tables with embedded SQL matching sql/schema.sql.

## Automated suite

Run the complete tester package: expected inventory is **25 tests**.

- DatabaseManagerTest: 2 tests for table creation and foreign-key enablement.
- UserDAOTest: 5 tests for create/find, uniqueness, authentication, and registered-user listing.
- TaskDAOTest: 6 tests for create/read, due-date/priority persistence, relevant-task query, update, completion, assignment/removal, and deletion.
- AuthControllerTest: 6 tests for registration/login, invalid login, logout, duplicate/blank registration, and registered-user retrieval.
- TaskControllerTest: 6 tests for creator/noncreator edit/delete rules, assignee completion, and unrelated-user completion denial.

Each test uses a temporary SQLite database. Record the actual run count, errors, and failures before final demonstration or submission.

## Manual acceptance checklist

The testing report records M01 duplicate registration, M02 shared task creation, M03 assignee visibility, M04 edit denial, M05 delete denial, M06 assignee completion, M07 blank title, M08 invalid date, and M09 full restart persistence.

For a new verification session, also check registration password confirmation, successful creator editing/deletion with confirmation, logout/login navigation, self-assignment and Unassigned, clearing a due date, default MEDIUM priority, completion reopening, and unrelated-task exclusion. These additional checklist items are not claims of separately recorded manual passes.

## Verified session lifecycle regression

1. Log in as the creator and leave both New Task and Edit Task windows open.
2. Click Logout on the dashboard. Both editors and the dashboard must close.
3. Log in as another account and confirm the old editors are absent.

Manual regression check M10 passed: the user confirmed that both New Task and Edit Task windows closed automatically when Logout was clicked. The subsequent screenshot shows Amanda's dashboard without the old editors. This is manual GUI evidence, separate from the JUnit rerun.

The post-fix Eclipse JUnit rerun on October 16, 2026 completed 25/25 tests with 0 errors and 0 failures in 0.413 seconds, as shown in the 11:33 PM screenshot. The existing suite still contains 25 methods; no new automated GUI test was added.

## Coverage and reporting

The current suite has no dedicated automated assertions for default MEDIUM priority, blank task titles, completion reopening, or open-editor/logout behavior. Blank-title rejection has manual evidence in M07. Keep the Week 2 JT/AT plans distinct from the final methods and M01-M10 observations. Record actual outcomes and remaining limitations before final submission.


## Latest final audit

Final audit addendum dated October 16, 2026. This addendum supersedes current-suite counts and coverage-limit statements above while preserving historical results and Week 1/Week 2 plans. The expanded Eclipse JUnit 5 suite passed 28/28 tests with 0 errors and 0 failures in 0.444 seconds. Class counts are DatabaseManagerTest 2, UserDAOTest 5, TaskDAOTest 6, AuthControllerTest 6, and TaskControllerTest 9. Three new controller tests cover rejection of every public task operation while logged out, self-assignment without duplicate dashboard results with MEDIUM default and optional date, completion and reopening followed by unassignment, and blank-title create/edit rejection with saved data unchanged. These are automated controller/SQLite checks, not GUI passes. Editor-window cleanup remains covered by the previously recorded manual M10 only. The fresh Problems tab reported 0 items in its current configuration. Production sources compiled on JDK 11.0.30 with --release 11 and -Xlint:all without diagnostics. Eclipse launched Main and reported database initialization success, but native automation could not bind to the Java window; no new manual GUI pass is claimed. See docs/FINAL_AUDIT.md and docs/FINAL_SUBMISSION_CHECKLIST.md for delivery status and limitations.
