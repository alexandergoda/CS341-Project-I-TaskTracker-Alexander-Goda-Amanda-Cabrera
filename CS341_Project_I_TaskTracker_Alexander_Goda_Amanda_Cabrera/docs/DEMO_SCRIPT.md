# Task Tracker demo and rehearsal

Prepared October 16, 2026. This is a proposed sequence, not a claim that rehearsal has occurred. Allow about 8–10 minutes for slides and demo, adjusting to the instructor’s actual limit.

## Preparation

- Import the Eclipse project, select JavaSE-11, add the exact JDBC dependency documented in lib/README.txt, and verify JUnit 5.
- Run the tester package. Current suite inventory is 28 tests across five classes.
- Run tasktracker.Main from the project directory so tasks.db resolves consistently.
- Use unique disposable local accounts such as demo_creator_<session>, demo_assignee_<session>, and demo_other_<session>. Choose throwaway demo-only passwords, not real account passwords. Do not change existing accounts.
- Rehearse on a separate copy of the project with an isolated tasks.db. Keep the real project database intact.
- Keep GitHub, the presentation, the design diagrams, and the latest JUnit result accessible. Confirm projector readability and timing.

## Demonstration sequence

1. **Introduction (Alexander, proposed):** explain the local shared-account task manager and selected due-date/priority feature. Show scope and MVC slides.
2. **Registration:** create the unique creator and assignee accounts. Show password mismatch feedback with disposable input, then correct it. Close registration and log in separately. Attempt duplicate registration and show rejection. Attempt a wrong login and show the generic error.
3. **Create:** log in as creator. New Task with a blank title must remain open and report a required title. Enter “Prepare team demo <session>”. Try an invalid date such as 10/20/2026, observe feedback, then use 2026-11-01. Assign the second account and choose HIGH. Save and identify creator, assignee, date, priority, and Open state.
4. **Creator edit:** change the title/description, save, and confirm the displayed changes. Edit again to select self as assignee and clear the due date. Confirm one relevant row, no date, and the chosen priority. Change assignee back to the second account.
5. **Permissions (Amanda, proposed):** log out and log in as assignee. Confirm assigned task visibility. Edit must be denied. Delete confirmation should not permit actual deletion by the assignee; show the controller denial and that the task remains. Use only the disposable demo task.
6. **Completion:** select the task, click Complete, and confirm Complete. Select it again and click Complete to reopen it. Explain the toggle despite the unchanged button label.
7. **Visibility:** log in as the third account. The shared task must not appear. Return to creator and show that it still appears.
8. **Persistence:** close the application normally, relaunch Main with the same working directory, and log in as assignee. Confirm saved details and status.
9. **Logout regression:** as creator, leave both a New Task window and an Edit Task window open. Click dashboard Logout. Confirm both editors and the old dashboard close. Log in as the other account and confirm no old editors remain. Do not save unsent drafts.
10. **Unassign and delete:** return as creator. Set Unassigned and save; the task remains in the creator view and disappears for the prior assignee. Create a small second task with no date and leave priority MEDIUM. As creator, cancel its delete confirmation once, then confirm deletion of this disposable task. Do not delete any pre-existing task.
11. **Testing and accountability:** show the latest full-suite result, explain temporary databases and representative error/permission tests, and show Amanda’s TaskController commit 63d8ae6. Explain each presenter’s actual contribution. Do not equate Insights with the complete author record.

## Rehearsal checklist

- [ ] Both team members review the deck and agree on their speaking parts.
- [ ] Each person can explain their code and one success/error path without reading.
- [ ] Entire sequence runs on the intended Mac with the intended JDK/JDBC driver.
- [ ] The deadline, total time limit, and required file formats are checked in Canvas.
- [ ] Logout closes both editor types, including when more than one editor is open.
- [ ] No real credentials or unrelated data appear on the projector.
- [ ] A timed run fits the allotted time with time for questions.
- [ ] Results are recorded as observed; a failed check is investigated before presentation.
- [ ] Disposable rehearsal data is handled only in the isolated rehearsal copy.

If a live action fails, stop and explain the observed problem. Use the design diagram and recorded test evidence to explain the intended behavior. Do not present a screenshot as proof of a fresh live pass. Fresh GUI repetition remains pending because this audit’s native automation could list Main but could not bind to the Java window.
