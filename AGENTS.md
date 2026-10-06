# Agent instructions for FerrisFeed

Read `docs/learnings.md` before changing anything. It records every
build/dependency/DI failure from the session that got CI green and the exact
fixes — violating it re-breaks the build in known ways.

Quick rules that matter most:
- Verify every Maven coordinate via `maven-metadata.xml` before adding it.
- AGP + Gradle wrapper + `compileSdk` move as a locked triple.
- `docs/tech-stack.md` holds version constraints (BOM floor, SDK levels).
- Write code against inventoried types only (`grep` declarations first).
- Full `assembleDebug` needs the Android SDK: verify config locally with
  `./gradlew help --no-daemon`, prove builds in CI (`./scripts/ci-watch.sh`).
