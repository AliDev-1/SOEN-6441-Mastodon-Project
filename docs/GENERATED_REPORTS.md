# Generated documentation and coverage

The delivery copies are stored in the repository so they survive `sbt clean`:

- [Java API documentation](generated/javadoc/index.html): production and test classes,
  private methods, constructors, parameters, return values, and authors.
- [JaCoCo HTML report](generated/coverage/html/index.html): coverage of handwritten
  production code, with source views.
- [JaCoCo XML report](generated/coverage/jacoco.xml): machine-readable counters.

GitHub displays HTML source rather than running the report interface. Clone or
extract the repository and open the HTML files locally in a browser. On macOS:

```sh
open docs/generated/javadoc/index.html
open docs/generated/coverage/html/index.html
```

## Regeneration

Select JDK 21 as described in [SETUP.md](SETUP.md), then run from the repository root:

```sh
sbt jacoco javadoc
mkdir -p docs/generated/javadoc docs/generated/coverage
rsync -a --delete target/site/javadoc/ docs/generated/javadoc/
rsync -a --delete target/scala-2.13/jacoco/report/ docs/generated/coverage/
```

`rsync --delete` synchronizes only these generated-report destinations, removing
outdated report files. It does not modify handwritten documentation. Regenerate
and recopy after Java or test changes; do not hand-edit generated HTML. Copy only
a successful build. The first command runs tests with coverage and generates
Javadoc; it does not contact Mastodon.

Expected current result: 88 passing tests, 14 production classes, 286/286 lines,
146/146 branches, and 55/55 methods covered. Compiler-generated routes and Twirl
classes are excluded from JaCoCo, but route tests exercise their rendered output.
The Javadoc build includes tests/private helpers and renders method-level author
entries through temporary source copies, without modifying the Java source files.
