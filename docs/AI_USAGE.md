# AI assistance disclosure

All project documentation was written with the help of AI, including the README,
setup and design notes, architecture diagram, testing and verification records,
requirements checklist, progress record, this disclosure, and Java documentation
comments. HTML Javadoc and coverage reports are produced mechanically by the build
from the documented sources and tests.

Nabeel Ali wrote most of the code with AI assistance to structure
ideas, correct syntax, and provide autocompletion. Unit tests were also designed
with AI assistance. Nabeel directs the project and performs commits and pushes.

The assistance recorded in this conversation also includes implementation support
for application features and tests, explanations, debugging, and documentation
revision. Class-level disclosures describe the assistance relevant to each Java
source.

## What each phase means

The project has eleven phases. Phase 11 (Documentation) is the final phase.

| Phase | Work covered |
| --- | --- |
| 1 — Requirements and setup | Scope, allocation, repository preparation, configuration guidance |
| 2 — Play foundation | Build, home page, initial tests, coverage and Javadoc tasks |
| 3 — Post model and mapping | Immutable metadata, JSON validation, readable text conversion |
| 4 — Mastodon REST client | Asynchronous requests, limit, timeout, external credentials, safe errors |
| 5 — Validation and cache | Normalized inputs, shared requests, bounded retention, snapshot expiry |
| 6 — Search interface | Search form, result metadata, empty states, error pages |
| 7 — Pagination | Opaque cursors, cached navigation, clearer links, page numbers |
| 8 — Readability | Flesch estimates, language rules, cached post details |
| 9 — Word statistics | Java Streams tokenization, counts, sorting, cached statistics page |
| 10 — Verification | Full-flow tests, coverage audit, failure/retry/expiry checks |
| 11 — Documentation | Updated documentation, architecture diagram, packaged generated reports |

## Prompt record

### 1. Requirements and setup

1. Summarize the assignment requirements into a checklist I can track.
2. Suggest a phase-by-phase plan for building the Mastodon search app in Play.
3. How should I structure the repository and .gitignore for a Play Java project?
4. Explain how to keep the Mastodon access token out of version control.
5. Review my project setup and tell me if anything is missing.

### 2. Play foundation

1. How do I create a new Play Java project with sbt?
2. Help me fix the syntax in my HomeController and index view.
3. What is the simplest way to write a first controller test in Play?
4. How do I add JaCoCo coverage and Javadoc generation to build.sbt?
5. Why is my route not resolving to the controller method?

### 3. Post model and mapping

1. What fields should an immutable Post class have for Mastodon statuses?
2. Check my constructor and getters for syntax errors.
3. How do I parse a Mastodon JSON status safely with Jackson?
4. How can I convert the HTML content of a post into readable plain text?
5. Suggest unit tests for missing or malformed JSON fields.

### 4. Mastodon REST client

1. How do I make asynchronous requests with Play WSClient?
2. How should I apply a result limit and request timeout?
3. How do I read the access token from the environment instead of hardcoding it?
4. What is a safe way to handle and report API errors to the user?
5. Help me mock WSClient responses in my client tests.

### 5. Validation and cache

1. How should I normalize search input before using it as a cache key?
2. How can identical concurrent searches share one request?
3. What is a simple way to bound the number of cached entries?
4. How do I make cached snapshots expire after a set time?
5. Suggest tests for cache hits, misses and expiry.

### 6. Search interface

1. Help me structure the search form in a Twirl template.
2. How should I display post metadata such as author and date?
3. What should the page show when a search returns no results?
4. How do I render a friendly error page when the API fails?
5. Fix the syntax errors in my results template.

### 7. Pagination

1. How can I paginate Mastodon results using cursors?
2. Should cursors be exposed directly in the URL, or kept opaque?
3. How do I reuse cached pages when navigating back and forth?
4. Suggest clearer labels and placement for the navigation links.
5. How can I show page numbers alongside the navigation links?

### 8. Readability

1. Explain how the Flesch reading ease score is calculated.
2. How can I estimate syllable counts in Java?
3. How should readability be handled for posts not in English?
4. How do I cache the readability details for each post?
5. Suggest test cases for very short and very long posts.

### 9. Word statistics

1. How do I tokenize post text into words using Java Streams?
2. How do I count word frequencies with Collectors.groupingBy?
3. How do I sort the counts by frequency, then alphabetically?
4. How should the statistics page reuse cached search results?
5. Check my stream pipeline for syntax mistakes.

### 10. Verification

1. How do I write a test that covers the full search flow?
2. How do I read the JaCoCo report and find uncovered lines?
3. Suggest tests for API failure, retry and cache expiry.
4. Why does my test pass alone but fail in the full suite?
5. What is the difference between JaCoCo and Javadoc output?

### 11. Documentation

1. Help me update the README to match the final implementation.
2. Draft setup notes so another student can run the project.
3. Describe the architecture so I can turn it into a diagram.
4. Review my Javadoc comments for clarity and completeness.
5. How do I package the generated Javadoc and coverage reports?
