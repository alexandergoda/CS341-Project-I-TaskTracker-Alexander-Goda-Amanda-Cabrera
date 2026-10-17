# Final Project I submission checklist

Prepared October 16, 2026 against the criteria supplied by the user and the existing course documents. The current Canvas assignment, exact deadline, time limit, accepted formats, and upload grouping were not independently accessible in this audit. Confirm them before uploading. No Canvas submission or instructor message has been sent.

## Deliverable inventory

| Deliverable | Exact location in project | Status |
| --- | --- | --- |
| GitHub URL | https://github.com/alexandergoda/CS341-Project-I-TaskTracker-Alexander-Goda-Amanda-Cabrera | Repository exists; verify final pushed hash |
| Java implementation and JavaDoc source | src/tasktracker/ | Ready; public class/behavior comments present; some accessor/constructor comments are sparse |
| JUnit 5 tests | test/tester/ (five classes) | 28/28 passed, 0 errors/failures |
| Introduction, user requirements/use cases, SRS | docs/Requirements/CS341_Project_I_Requirements_Analysis_Alexander_Goda_Amanda_Cabrera.docx | Ready with latest test addendum |
| MVC, schema, UML and historical test plans | docs/Design/CS341_Project_I_Week2_Design_Package_Alexander_Goda_Amanda_Cabrera.docx | Ready with latest test addendum |
| Standalone diagrams | Four PNGs in docs/Design/ | Match embedded figures |
| Testing report | docs/CS341_Project_I_Testing_Report_Alexander_Goda_Amanda_Cabrera.docx | Ready with 28-test addendum; historical M01–M10 preserved |
| Presentation | docs/CS341_TaskTracker_Presentation.pptx | Prepared for team review; live rehearsal remains |
| Demo and rehearsal plan | docs/DEMO_SCRIPT.md | Prepared; checklist not yet completed |
| Contribution accountability | Git history, TEAM_WORKFLOW.md, presentation notes | Amanda commit verified; team must confirm non-code work and speaking roles |
| Submission archive if requested | Clean final ZIP in audit outputs | Prepare from final commit; excludes local/generated artifacts |

## Before submission

- [ ] Open the actual Canvas assignment and confirm the deadline and any latest direct instructor guidance.
- [ ] Confirm whether DOCX, PDF, PPTX, ZIP, or separate items are required. Do not assume a single ZIP replaces separate uploads.
- [ ] Confirm the repository URL is accessible to Professor Cornez and the expected branch is main.
- [ ] Confirm both team members’ names and required contribution/peer-evaluation statements.
- [ ] Review the slide deck together and complete the timed rehearsal in DEMO_SCRIPT.md.
- [ ] Repeat the GUI regression list, especially editor cleanup, on the presentation machine.
- [ ] Confirm the latest commit is pushed and local/remote hashes match.
- [ ] If code changes, rerun the suite and revise the actual count/results before packaging.
- [ ] Verify ZIP opens and contains src, test, sql, lib/README.txt, project configuration, and final documentation.
- [ ] Exclude tasks.db and other databases, JARs, bin, .class, .DS_Store, Git metadata, and unrelated files.
- [ ] Follow lib/README.txt to obtain the JDBC driver on another machine; the dependency is intentionally not bundled.
- [ ] Upload only after the user explicitly requests Canvas submission, then verify the receipt.

The introduction and SRS are sections of the Requirements Analysis. UML and schema are inside the Week 2 Design Package and also supplied as PNGs. If Canvas requests them separately, extract/export the relevant sections rather than creating inconsistent duplicate specifications. No generated JavaDoc HTML is included unless the assignment specifically asks for it; source comments remain in the code.
