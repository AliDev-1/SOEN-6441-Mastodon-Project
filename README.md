# MAWA Mastodon Analysis Web App

SOEN 6441 Advanced Programming Practices, Delivery 1. MAWA will retrieve
Mastodon hashtag posts and display them with readability scores and word statistics.

## Current status

Phase 4 adds an asynchronous Mastodon client with a 40-post request limit,
timeouts, and controlled errors. A live request successfully retrieved and mapped
40 posts. The home page still shows the application foundation; the search UI,
caching, and analysis follow in later phases.

## Run locally

Select JDK 21, then start Play from the repository root:

```sh
sbt run
```

Open <http://localhost:9000>. Stop the server with Ctrl+C.
See [setup instructions](docs/SETUP.md) for selecting the JDK on macOS.

## Test and document

```sh
sbt test
sbt jacoco
sbt javadoc
sbt clean
```

Tests run without Mastodon credentials or network access to Mastodon. Coverage
reports appear at `target/scala-2.13/jacoco/report/html/index.html`, and Javadoc at
`target/site/javadoc/index.html`. Javadoc includes private methods and test classes.
`clean` deletes generated outputs, so regenerate reports afterward when needed.
See [testing notes](docs/TESTING.md) for the current test cases and coverage rules.

## Planned features

- Hashtag search with readable post content and required metadata.
- Additional results through Mastodon cursor pagination, at most 40 posts per request.
- Cached results shared by the search and analysis pages.
- Flesch Reading Ease and Flesch–Kincaid Grade Level, with a post details page.
- A linked word-frequency page implemented with Java Streams.

The application will use Java 21, Play Java, sbt, and ordinary server-rendered pages.
Controller actions and HTTP calls will be asynchronous. No actors, WebSockets,
or reactive streaming will be implemented for Delivery 1.

## Documentation

- [Setup instructions](docs/SETUP.md)
- [Requirements and submission checklist](docs/REQUIREMENTS.md)
- [Implementation phases](docs/PROGRESS.md)
- [Design and data mapping](docs/DESIGN.md)
- [AI assistance disclosure](docs/AI_USAGE.md)

## Development workflow

Implement one phase at a time, including its tests and documentation. Review the
changes and verification results before moving on. Nabeel Ali performs all commits
and pushes manually; the assistant must not commit, push, or create release tags.

## Configuration

The client reads `MASTODON_INSTANCE` and `MASTODON_ACCESS_TOKEN` when it is
created. Missing settings produce a controlled error on fetch without preventing
the home page from starting. Use a dedicated project account and a user access
token with `read:statuses`. Never commit credentials. See the setup instructions
for loading `.env` before starting sbt. Restart after changing these values.
The instance must be an HTTPS origin without an API path, query, or fragment.

## Project ownership

Nabeel Ali implements the group component and readability as the primary individual
feature. Word statistics is an additional feature.
