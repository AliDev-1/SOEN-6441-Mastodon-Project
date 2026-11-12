# AI assistance disclosure

Nabeel Ali will write most of the application code, with Codex assisting
with ideas, explanations, logic review, syntax checks, debugging, and test design.
This describes the planned workflow; the record below will reflect the assistance
actually used as implementation progresses.

OpenAI Codex assisted with interpreting the assignment, planning the implementation,
and drafting the Phase 1 repository documentation and configuration examples.
Phase 2 assistance covered implementing the initial Play build, asynchronous home
controller, template, stylesheet, and tests. Later contributions will be recorded
as they occur.

## Prompt record

The following are concise summaries of the developer's instructions, not verbatim
copies of the complete conversation:

| Phase | Instructions | Assisted work |
| --- | --- | --- |
| Planning | Examine the project instructions and lecture material; plan a concise, student-readable Java implementation with tests, comments, documentation, and incremental Git history. | Requirements review, architecture, tests, and implementation checkpoints. |
| Scope | Implement the group component, readability, and word statistics. | Selected feature scope and ordering. |
| 1 | Implement the agreed plan one phase at a time; leave all commits and pushes to the developer. | README, setup instructions, requirements checklist, progress record, Git exclusions, and environment example. |
| 2 | Move on to Phase 2 after the manual Phase 1 commit and push. | Play skeleton, build configuration, JDK setup, home page, controller and route tests, coverage and Javadoc tasks, and documentation updates. |
| 3 | Implement the post domain model, JSON mapping, and readable text conversion with tests. | Implementation and test-design assistance for post values, mapping validation, HTML conversion, and related documentation. |
| 4 | Implement the asynchronous REST client, error handling, mocked tests, and a separate live connection check. | Implementation and test-design assistance for requests, configuration, safe errors, dependency injection, and documentation; a live smoke check without displaying credentials. |

## Ongoing disclosure

Update this record as code is implemented. Each assisted Java class, including test
classes, must disclose the assistance and summarize its relevant prompt in Javadoc.

