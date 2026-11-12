# Delivery preparation and demonstration

This is a preparation checklist, not a claim that submission or external approvals
have been completed. Nabeel Ali handles commits, pushes, submission, and signatures.

## Prepared artifacts

| Artifact | Location |
| --- | --- |
| Java implementation and views | `app/` |
| Tests | `test/` |
| Build, routes and configuration | `build.sbt`, `project/`, `conf/` |
| Styles | `public/` |
| Installation, use and limitations | `README.md`, `docs/SETUP.md` |
| Architecture diagram and concise overview | `docs/ARCHITECTURE.md` |
| Detailed design and analysis rules | `docs/DESIGN.md` |
| Testing partitions and verification | `docs/TESTING.md`, `docs/VERIFICATION.md` |
| AI assistance and prompt provenance | `docs/AI_USAGE.md` |
| Generated Javadoc and coverage | `docs/generated/` |
| Requirement status and progression | `docs/REQUIREMENTS.md`, `docs/PROGRESS.md` |

## Phase 12 — Final delivery checks still to perform

- Verify a clean checkout resolves dependencies and runs all required sbt tasks.
- Start the app with external credentials and visually check home, results,
  pagination, details, statistics, and error states on desktop and a narrow window.
- Confirm the dedicated project account and token scope without exposing credentials.
- Inspect the final archive: include source/build/docs; exclude `.env`, local
  credentials, `.git`, disposable build outputs, editor metadata, and runtime logs.
- Verify HTML reports open and local documentation links resolve after extraction.
- Confirm repository URL and individual task allocation were submitted as required.
- Obtain and genuinely sign the official Expectation of Originality form; include
  it in the submission. No signature or approval is supplied by the assistant.
- Confirm the submission format and demonstration arrangements with the course.

The repository requirements checklist remains authoritative for verified versus
unverified items. Do not mark an external action complete from prepared instructions.

## Demonstration sequence

1. Explain the chosen scope: group application, readability as the primary individual
   task, and word statistics as the additional feature.
2. Start `sbt run`, search a hashtag without `#`, and identify all metadata fields.
3. Open older posts; point out the page number and return to the previous saved page.
4. Open post details and explain the two scores, counts, English assumptions, and
   saved-page return link.
5. Select the hashtag above results and explain total versus unique words, descending
   frequencies, ties, and the current-page analysis scope.
6. Show an invalid hashtag such as `#java`, and explain empty-results and expired-link
   handling. Do not alter credentials or wait for expiry during the demo; automated
   tests demonstrate timeout, authentication, rate-limit and expiry cases safely.
7. Run `sbt jacoco`, open the coverage report, and explain why coverage is not proof
   of correctness. Show a full-flow test and its mocked outbound HTTP boundary.
8. Open Javadoc and the architecture diagram. Explain the REST-independent model
   and the additional components a future streaming version would need.

No credentials should appear in shared screens, recordings, or submission files.
