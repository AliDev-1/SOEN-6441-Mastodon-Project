# Testing strategy

Run `sbt test` for JUnit tests and `sbt jacoco` for tests with coverage. The tests
use an isolated Play application and never contact the live Mastodon API.
Mockito is available for the client and service tests added in later phases.

## Phase 2 cases

| Case | Input or partition | Expected result | Test |
| --- | --- | --- | --- |
| Direct home action | No input parameters | Completed response with status 200, HTML content type, exact title, heading, language, and status text | `indexReturnsHomePage` |
| Home route | `GET /` | Routing and dependency injection return the same specified home page | `homeRouteReturnsHomePage` |
| Stylesheet | Asset URL extracted from the home page | Status 200, CSS content type, expected font rule | `homeStylesheetIsServed` |

The home action has one input partition because it accepts no parameters and has
no conditional branches. Hashtag, API response, cache, and analysis partitions will
be added alongside those features. Tests assert specified values, not just nonempty
responses. Future waits are confined to test code.

## Coverage rules

JaCoCo measures all compiled application classes except compiler-generated routes,
reverse routes, and Twirl templates. No handwritten production class is excluded.
The build requires 100% line and method coverage and zero missed branches.

The sbt plugin treats a class with no branches as 0% branch coverage. The extra
XML report check instead rejects any missed branch, allowing branch-free classes
without weakening the requirement once conditional logic is added. Branch coverage
for the initial home controller is therefore not applicable, rather than a measured
100% over existing branches.

HTML and XML coverage outputs are under `target/scala-2.13/jacoco/report/`.
Generated code is still exercised through route and response tests even though it
is not included in handwritten-code coverage.

## Javadoc verification

Run `sbt javadoc`. This compiles the application and tests, then documents all
handwritten Java sources together with `-private`. Standard Javadoc does not render
method-level `@author` entries. The task translates those tags in temporary source
copies into a custom author tag, leaving the original Java files unchanged.
Documentation warnings fail the task.
Inspect `target/site/javadoc/index.html` and confirm the test class and its private
`assertHomePage` helper are included.
