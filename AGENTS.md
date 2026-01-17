# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/org/red5/io/moq/` holds core library code.
  - `cmaf/`, `loc/`, `moqmi/`, and `hang/` contain format-specific models and serializers.
- `src/test/java/org/red5/io/moq/` contains JUnit 5 tests, grouped by format.
- `docs/` stores MoQ-related draft specs and reference texts.
- `target/` is Maven build output (generated).

## Build, Test, and Development Commands
- `mvn clean package` compiles and packages the jar; runs tests by default.
- `mvn test` runs the full JUnit test suite.
- `mvn test -Dtest=LocObjectTest` runs a single test class.
- `mvn clean package -DskipTests` builds without executing tests.

## Coding Style & Naming Conventions
- Java 21 source in `src/main/java`; follow standard 4-space indentation and braces-on-same-line style seen in the codebase.
- Class names are `UpperCamelCase`, methods/fields are `lowerCamelCase`.
- Package naming mirrors format areas: `org.red5.io.moq.{cmaf|loc|moqmi|hang}`.
- No explicit formatter or linter is configured; keep changes consistent with surrounding files.

## Testing Guidelines
- Tests use JUnit Jupiter (JUnit 5) via Maven Surefire.
- Test classes follow `*Test` naming (e.g., `CmafFragmentTest`, `MoqMIObjectTest`).
- Add or update tests for new format features or serialization changes.

## Specification Compliance
- Authoritative references live in `docs/` (e.g., `docs/draft-wilaw-moq-cmafpackaging-01.txt`, `docs/draft-ietf-moq-loc-01.txt`, `docs/draft-cenzano-moq-media-interop-03.txt`).
- When overlapping guidance exists, any IETF draft/spec in `docs/` overrides non-IETF drafts.
- Changes must align with the relevant draft; call out spec sections in PRs when behavior changes.
- Add tests that demonstrate required features and edge cases mandated by the specs.

## Commit & Pull Request Guidelines
- Recent commits use short, imperative summaries (e.g., “Add ...”, “Update ...”).
- Keep commits scoped to a single concern; mention affected format/module.
- PRs should include a concise description, relevant spec links if applicable, and test results (command + outcome).

## Configuration Notes
- Java 21 and Maven 3.8+ are required; dependencies are defined in `pom.xml`.
- Release profile exists but is not required for local development.
