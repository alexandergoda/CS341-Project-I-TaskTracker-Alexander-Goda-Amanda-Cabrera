# CS341 Project I finalization audit

Audit dated October 16, 2026 using the client course date. Team: Alexander Goda and Amanda Cabrera. Instructor: Professor Trish Cornez.

The core application is implemented, the Java 11 build passes, the expanded JUnit suite passes, and the documentation describes the implementation. Delivery preparation now includes an actual reviewed presentation and a demo/submission plan. Final readiness still depends on a fresh manual GUI rehearsal and confirmation of the actual Canvas assignment. This report does not claim exhaustive testing or predict a grade.

## Scope and verification method

Read the production Java sources and five test files from disk, including Main, User, Task, Priority, DatabaseManager, UserDAO, TaskDAO, AuthController, TaskController, all four Swing views, and both package-info files. Inspected README, TEAM_WORKFLOW, implementation status, validation checklist, schema.sql, dependency instructions, Eclipse project/classpath/settings and ignore rules. Extracted and read Requirements Analysis, Design Package, and Testing Report text, reviewed all four diagrams, and verified embedded images match the standalone files byte for byte. Rendered the revised DOCX files and inspected page layouts and new addenda. Eclipse was used for suite execution, startup and Problems inspection; Safari for GitHub authorship and repository inspection. The word-for-word source review used direct file reads, not opening every source line individually in Eclipse.

The course comparison uses the user-supplied Project I criteria, known lecture guidance, and existing course-derived documentation. The cached original chat was also read. Original lecture PDFs/current Canvas assignment were not independently available in the inspected project materials. Any later direct instruction from Professor Cornez takes precedence.

## Fresh automated results

| Verification | Observed result |
| --- | --- |
| Original 25-test suite rerun in Eclipse | 25/25, 0 errors, 0 failures, 0.437 seconds |
| Expanded full suite in Eclipse | **28/28, 0 errors, 0 failures, 0.444 seconds** |
| DatabaseManagerTest | 2 tests |
| UserDAOTest | 5 tests |
| TaskDAOTest | 6 tests |
| AuthControllerTest | 6 tests |
| TaskControllerTest | 9 tests |
| Production compile | JDK 11.0.30, --release 11, -Xlint:all, exit 0, no diagnostics |
| Eclipse Problems view | **0 items** in the current view configuration; no displayed errors/warnings |
| Startup from Eclipse | Main launched using JDK 11 and printed database initialization success for tasks.db |

The Problems view is scoped by its existing configuration and does not establish that unrelated workspace projects are free of problems. The compiler check independently verifies the production source. Tests use JUnit 5 and temporary SQLite files. They exercise controller/DAO integration, not automated Swing GUI interaction.

Three focused TaskControllerTest methods were added:

- loggedOutUserCannotReadOrChangeTasks: all public task reads/mutations reject an ended session, and persisted task data remains unchanged.
- creatorCanReopenSelfAssignedTaskAndRemoveOptionalValues: self-assignment returns one relevant task, MEDIUM is the omitted-priority default, due date is optional, complete/reopen persists, and unassignment removes assigned membership while retaining creator visibility.
- blankTitleCannotCreateOrOverwriteTask: whitespace titles fail both create and edit, without adding a row or overwriting the saved title/assignee.

## Manual acceptance status

**No new manual GUI acceptance check is claimed in this audit.** Eclipse successfully launched Main, but native automation listed the running Java application and then rejected bindings by app name, bundle ID, and executable path. A terminal-launched Swing process also exited after initialization; the successful Eclipse launch is separate evidence. This prevents honest repetition of registration/login, task CRUD, assignment, validation, persistence-through-GUI-restart and editor cleanup in this session.

The prior testing report retains these ten recorded checks, which were reviewed but not rerun here:

| Historical check | Recorded result |
| --- | --- |
| M01 duplicate registration | PASS |
| M02 assigned task with date and HIGH priority | PASS |
| M03 assignee visibility | PASS |
| M04 noncreator edit denial | PASS |
| M05 noncreator delete denial | PASS |
| M06 assignee completion | PASS |
| M07 blank title validation | PASS |
| M08 invalid date-format validation | PASS |
| M09 full application restart persistence | PASS |
| M10 both New Task and Edit Task windows close on logout | PASS, user-confirmed in prior session |

The fresh DEMO_SCRIPT.md explicitly lists the additional GUI workflows to repeat, including self-assignment, unassignment, cleared dates, default priority, reopening, unrelated-account exclusion, creator deletion with confirmation/cancellation, and editor cleanup. No test users/tasks were added to the live database by this audit. Supplemental database fixtures were isolated under the audit workspace.

## Database and portability

The live users/tasks schema exactly matches a fresh database initialized by the production DatabaseManager. It also matches sql/schema.sql and the diagram. users has an integer generated primary key, unique non-null username and non-null password_hash. tasks has generated task_id, non-null title and creator, optional description/assignee/date, MEDIUM priority default with LOW/MEDIUM/HIGH CHECK, completion default 0 with 0/1 CHECK, and both user foreign keys.

- Live PRAGMA integrity_check returned ok. Live PRAGMA foreign_key_check returned no violations.
- A Java 11/JDBC harness initialized an isolated database twice, inserted data, initialized again and confirmed data remained. It verified connection foreign_keys=1 and the expected defaults.
- Additional in-memory reference-schema checks verified username uniqueness, invalid creator FK rejection, priority CHECK and completion CHECK. These supplemental checks are separate from the 28 JUnit tests.
- tasks.db resolves relative to the working directory; startup executes embedded SQL, not the reference schema.sql file.
- No database, JAR, compiled class or .DS_Store is tracked. bin and database/JAR patterns are ignored. The pre-existing parent ../.DS_Store remains untracked.
- Eclipse paths are relative and Java compliance/source/target are 11. A new checkout needs the Xerial JDBC JAR named in lib/README.txt and Eclipse’s JUnit 5 container. README now documents setup and launch.

## Code and lecture guidance

MVC/OOP boundaries are clear: domain objects hold state, DAOs own parameterized SQL and resource cleanup, controllers enforce current-user permissions, and Swing views collect input/display results. Main orchestrates startup. EventQueue.invokeLater starts Swing. Views separate initialize and createEvents and use explicit ActionListeners. Production and tests live under separate source folders/packages. Week 1/Week 2 plans remain historical. Requirements use user-facing wording, use cases include normal/error paths, and UML shows representative relationships without algorithmic bodies.

The logout fix is present: dashboard editor tracking, windowClosed removal, snapshot-based disposal, and disposal before clearing the authenticated session. No further production defect was established that justified redesign. The new tests add confidence at the controller boundary without changing application behavior.

Course-appropriate limits remain: fixed Swing layouts, synchronous JDBC calls in UI handlers, sparse comments on some simple accessors/constructors, and unsalted SHA-256 password hashes. The latter is accurately documented and is not presented as production-grade password storage. These limits do not justify feature creep for the current local assignment. Tests do not establish exhaustive error/concurrency/security/accessibility coverage.

## Requirements and rubric assessment

PASS means evidence supports the stated implemented area, not exhaustive coverage or a guaranteed score. PARTIAL means an evidence/delivery gap remains.

| Area | Status | Evidence and remaining work |
| --- | --- | --- |
| Registration, login/logout | PASS | Source and automated authentication checks; historical GUI evidence |
| Create/edit/delete and ownership | PASS | DAO/controller tests, including blank-title protection; historical GUI permission checks |
| Assignment and relevant visibility | PASS | Registered-user FK, relevance query, self-assignment/unassignment tests |
| Completion and reopening | PASS | Permission tests and new persisted creator reopen test |
| Due dates and priority extra feature | PASS | Persistence tests, defaults check, historical date validation |
| SQLite/JDBC, automatic DB/tables | PASS | Actual live schema, fresh/repeat initialization and constraints verified |
| Java 11+, JUnit 5 | PASS | Clean Java 11 compile and fresh 28-test Eclipse run |
| MVC and OOP course style | PASS | Modular layers, EDT startup, constructor orchestration, separate event wiring |
| Introduction, user requirements, SRS, use cases, UML | PASS | Present and reconciled; course planning retained |
| Testing report and focused verification | PASS | Updated actual count/coverage and evidence limits; no invented manual passes |
| Fresh GUI regression repetition | PARTIAL | Automation cannot bind the Java window; complete rehearsal checklist |
| Meaningful team contribution | PASS | Amanda TaskController commit 63d8ae6 verified in Git and Safari |
| Regular balanced contribution accountability | PARTIAL | One separate Amanda implementation commit verified; regular participation/non-code work must be confirmed by team, not inferred from chart |
| Requirements/design, 25% | PASS | Requirements/SRS/use cases, architecture/schema/UML and traceability available |
| Implementation, 30% | PASS | Features implemented and automated checks pass; fresh GUI repetition pending |
| Testing/report, 20% | PASS | Focused 28-test suite plus clearly labeled prior acceptance results |
| Code quality/OOP, 15% | PARTIAL | Architecture/style supports criteria; some public member JavaDoc remains sparse |
| Presentation/demo, 10% | PARTIAL | Actual 10-slide PPTX, notes and script prepared; team review/timed rehearsal not performed |
| Exact Canvas submission/deadline | REMAINING | Current assignment/upload grouping/deadline not independently verified; nothing submitted |

## Changes and Git history

Initial finalization commit: ec1bfa0, “Fix task editor cleanup on logout and reconcile final documentation”. It contains exactly the original 11 legitimate modified files: TaskDashboardView, README/status/checklist, three DOCX files and four diagrams. The later test-evidence revision adds three controller tests, addenda in the three DOCX and three Markdown documentation files, and clean-checkout instructions. Delivery materials are committed separately when practical. See the final delivery report for the pushed hashes and verified remote state.

Amanda’s original commit 63d8ae6d64f1030a5acf54e4b8e73f5925790532 remains an ancestor of main and retains Amanda Cabrera as author. Safari’s GitHub commit page independently displays her name and the TaskController contribution. GitHub Insights showed only Alexander during inspection, so no complete contributor statistics or equal contribution claim is made. No history was rewritten and no force-push was used.

## Delivery readiness and remaining files

The presentation exists as docs/CS341_TaskTracker_Presentation.pptx: 10 slides with speaker notes, actual design figures, test evidence, logout regression, demo sequence and contribution accountability. All slides were rendered and reviewed; package/layout/import checks passed. It has not been rehearsed by the team or verified in native PowerPoint.

Submit the repository URL and, according to Canvas’s actual grouping, the Requirements Analysis DOCX, Week2 Design Package DOCX with diagrams, Testing Report DOCX, Java/JUnit sources and presentation PPTX. The generated clean ZIP contains the final tracked project files and excludes local artifacts. The introduction and SRS are inside the Requirements Analysis. Confirm whether separate PDFs, individual UML files, peer evaluations or individual contribution statements are requested. FINAL_SUBMISSION_CHECKLIST.md gives exact paths.

Next required actions are team review, a timed GUI rehearsal on the presentation machine, confirmation of current Canvas instructions/deadline, and authorized upload with receipt verification. No Canvas upload or message to Professor Cornez was performed.
