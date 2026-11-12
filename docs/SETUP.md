# Development setup

These instructions prepare the environment and run the MAWA Play skeleton.

## Java and sbt

Use JDK 21 and sbt. Play lists Java 21 among its supported LTS versions in its
[requirements](https://www.playframework.com/documentation/3.0.x/Requirements).
Install JDK 21 from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21)
if necessary. Install sbt using its [official instructions](https://www.scala-sbt.org/download/).

JDK 21 was installed through Homebrew during Phase 2. On this Apple Silicon Mac,
select it for the current terminal without changing the system-wide Java default:

```sh
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

For a JDK installed and registered through the standard macOS installer, use:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
command -v sbt
```

Do not run the selection command until JDK 21 is installed. The build pins Play
3.0.12, sbt 1.13.0, Scala 2.13.18 (for Play templates), sbt-jacoco 3.7.0, JUnit
4.13.2, Mockito 5.24.0, and jsoup 1.21.2. Application logic is written in Java.

## Run and verify

From the repository root with JDK 21 selected:

```sh
sbt run
```

Visit <http://localhost:9000> to view the home page. Ctrl+C stops the server.
No Mastodon credentials are required for the home page. The initial dependency
download requires internet access; sbt resolves the pinned libraries automatically.

Run the complete build checks separately from the development server:

```sh
sbt clean test jacoco javadoc
```

Coverage is generated under `target/scala-2.13/jacoco/report/`; Javadoc is generated
under `target/site/javadoc/`. These build outputs are ignored by Git. Final
submission documentation will be copied to its delivery location in Phase 11.

## Git identity and review

The project repository is
[AliDev-1/SOEN-6441-Mastodon-Project](https://github.com/AliDev-1/SOEN-6441-Mastodon-Project).
Use your intended commit identity and check it from this repository:

```sh
git config --get user.name
git config --get user.email
git status --short
git diff --check
git diff
```

Review new files as well: `git diff` does not show untracked file contents.
Commit and push manually after each phase. Public remote readability does not
establish permission to push; authenticate through your normal GitHub workflow
when making the first push.

## Dedicated Mastodon account

1. Create a project-only account on a Mastodon instance accepting registration.
   Complete any email verification or approval. Do not use a personal account.
2. Sign in and open Preferences, then Development, then New application.
3. Name the application `MAWA`. Grant `read:statuses`; remove unnecessary scopes.
4. Save the application and open its details. Copy **Your access token**, which is
   the user token for the signed-in project account. Do not substitute the client
   ID or client secret.
5. Store the instance origin and token locally using the procedure below.

Menu names can differ between instances. Mastodon's
[client access documentation](https://docs.joinmastodon.org/client/token/)
describes application registration and token types. During Phase 4, the configured
client successfully retrieved 40 hashtag posts in a separate live smoke check.

## Local configuration

From the repository root:

```sh
touch .env
chmod 600 .env
```

Edit `.env` locally. Set `MASTODON_INSTANCE` to the HTTPS instance origin, without
an API path, and `MASTODON_ACCESS_TOKEN` to your user token. Never paste the token
into chat, source code, screenshots, or commit messages.

For a new setup, add these entries and replace the placeholders locally. If your
`.env` is already configured, keep its existing values.

```dotenv
MASTODON_INSTANCE='https://your-instance.example'
MASTODON_ACCESS_TOKEN=''
```

Load your own local file into the terminal that will run sbt:

```sh
set -a
source ./.env
set +a
```

This shell command executes the file: source only the `.env` you created and
reviewed. sbt does not load `.env` automatically. `.gitignore` excludes this local
configuration file.

The client captures settings when it is created: restart the application after
changing `.env`, loading the file in the terminal that starts sbt. The instance
must be an HTTPS origin such as `https://mastodon.social`, optionally with a
trailing slash, and without credentials, an API path, query, or fragment.

The current home page does not initiate requests. Once the search UI is added,
verify a hashtag search through the running app. Automated tests use mocks and
must not contact Mastodon.

## Before submission

Confirm the task allocation in Moodle, obtain the official
Expectation of Originality form, and confirm the TA demonstration arrangements.
