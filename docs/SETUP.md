# Development setup

These instructions prepare the environment for MAWA. The Play build will be added
in Phase 2; the commands for running the application will be verified then.

## Java and sbt

Use JDK 21 and sbt. Play lists Java 21 among its supported LTS versions in its
[requirements](https://www.playframework.com/documentation/3.0.x/Requirements).
Install JDK 21 from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21)
if necessary. Install sbt using its [official instructions](https://www.scala-sbt.org/download/).

On macOS, select the installed JDK 21 for the current terminal:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
command -v sbt
```

Do not run the selection command until JDK 21 is installed. The initial environment
inspection found JDK 25 and sbt, but no JDK 21. Phase 2 will pin and verify the Play,
sbt, and library versions together.

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
describes application registration and token types. Account creation and the live
connection check are still pending; they are not implied by these instructions.

## Local configuration

From the repository root:

```sh
cp .env.example .env
chmod 600 .env
```

Edit `.env` locally. Set `MASTODON_INSTANCE` to the HTTPS instance origin, without
an API path, and `MASTODON_ACCESS_TOKEN` to your user token. Never paste the token
into chat, source code, screenshots, or commit messages.

Load your own local file into the terminal that will run sbt:

```sh
set -a
source ./.env
set +a
```

This shell command executes the file: source only the `.env` you created and
reviewed. sbt does not load `.env` automatically. The tracked `.env.example`
contains placeholders only, and `.gitignore` excludes the real file.

After the client is implemented, manually verify a hashtag request using the
running app. Automated tests will use mocks and must not contact Mastodon.

## Before submission

Confirm the task allocation in Moodle, obtain the official
Expectation of Originality form, and confirm the TA demonstration arrangements.
