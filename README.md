# MAWA Mastodon Analysis Web App

SOEN 6441 Advanced Programming Practices, Delivery 1. MAWA will retrieve
Mastodon hashtag posts and display them with readability scores and word statistics.

## Current status

Phase 1 establishes the requirements and development setup. The Play application
has not been implemented yet, so build, run, test, and coverage commands are not
available at this checkpoint.

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
- [AI assistance disclosure](docs/AI_USAGE.md)

## Development workflow

Implement one phase at a time, including its tests and documentation. Review the
changes and verification results before moving on. Nabeel Ali performs all commits
and pushes manually; the assistant must not commit, push, or create release tags.

## Configuration

The planned application will read `MASTODON_INSTANCE` and `MASTODON_ACCESS_TOKEN`
from environment variables. Use a dedicated project account and a user access
token with `read:statuses`. Never commit credentials. See the setup instructions
for the local configuration procedure.

## Project ownership

Nabeel Ali implements the group component and readability as the primary individual
feature. Word statistics is an additional feature.
