# AGENTS.md

## Purpose

This repository is an Android project.

Your job as an agent is to restore a healthy Android build with minimal, targeted fixes and to keep working until a Debug APK is actually produced.

Primary goal:

1. Fix build failures
2. Ensure `./gradlew assembleDebug --stacktrace` succeeds
3. Ensure a Debug APK is actually produced
4. If CI is part of the failure chain, keep the workflow aligned with the local build path
5. Prepare a clean PR summary of the changes

Do not stop after fixing the first error.
Do not stop after fixing only the currently known errors.
Keep iterating until the Debug APK is produced or a true external blocker is proven.

---

## How to work in this repository

Use an iterative repair loop:

1. Inspect repository docs and build configuration
2. Determine the intended build path and environment
3. Run the relevant Gradle command
4. Identify the first real blocker
5. Fix the root cause
6. Re-run the build
7. Repeat until the APK is generated

Prefer minimal changes.
Fix root causes, not only surface symptoms.

Do not:
- stop after the first partial success
- only provide analysis without code changes
- comment out large code paths just to pass compilation
- remove `override` just to silence errors
- add fake stubs, fake repositories, or mock implementations unless the existing architecture clearly requires them
- delete business logic to bypass failures
- perform broad refactors unrelated to restoring a healthy build

---

## Plan expectations

Before making changes, inspect the repository and form a plan based on the real build setup.

Your plan should account for:

- repository documentation
- build configuration
- Gradle wrapper usage
- CI workflow behavior
- the actual APK output path
- known likely failure areas listed below

After the plan is approved or execution begins, continue directly into the fix/build loop.
Do not stop after plan creation.

---

## Repository inspection order

Before changing code, inspect these if present:

- `README.md`
- `.github/workflows/`
- `gradle/wrapper/gradle-wrapper.properties`
- `settings.gradle` / `settings.gradle.kts`
- root `build.gradle` / `build.gradle.kts`
- module `build.gradle` / `build.gradle.kts`
- `gradle/libs.versions.toml`

Determine:

- expected Gradle wrapper usage
- JDK / Gradle / AGP / Kotlin version compatibility
- intended build command
- actual APK output path
- whether CI uses the same build path as local development

---

## Gradle rules

Always prefer the Gradle wrapper.

Use:

```bash
./gradlew
```

Do not prefer plain:

```bash
gradle
```

Primary validation commands:

```bash
./gradlew :data-local:compileDebugKotlin --stacktrace
./gradlew assembleDebug --stacktrace
```

If the first command fails, fix those compiler errors first.
Then continue to the full APK build.

If wrapper scripts are missing but the repository clearly intends wrapper-based builds, restore or fix them.

---

## Debug APK expectations

The main success condition is that a Debug APK is actually produced.

Check common output paths such as:

```bash
app/build/outputs/apk/debug/
```

If the project uses a different module or output location, detect the real configured path and use that instead.

Do not declare success unless the APK file exists.

---

## CI expectations

If GitHub Actions or another CI workflow is present, keep it aligned with the local build process.

Preferred behavior:

- build with `./gradlew assembleDebug --stacktrace`
- upload the Debug APK from the real output path
- upload failure reports only when the build fails

If CI currently uses a different Gradle invocation and that is part of the failure chain, fix it.

Do not break failure-report uploads while fixing APK uploads.

If the task results in a PR, ensure the branch and workflow changes are consistent with passing CI.

---

## Known likely failure areas in this repository

Check these early to avoid wasting time:

1. Kotlin compile failures in `:data-local:compileDebugKotlin`
2. Repository layer out of sync with renamed fields, constructor parameters, DTOs, entities, mappers, or interfaces
3. Interface path or signature changes causing `override` mismatches
4. Bad imports or moved packages
5. Gradle / AGP / Kotlin version mismatch
6. CI using the wrong Gradle command or not using the wrapper
7. APK artifact path mismatch in CI upload steps

Known examples previously observed:

- `volumeKeysPaging` unresolved or no longer present
- `ProgressRepository` unresolved, moved, or renamed
- `getProgress` / `saveProgress` override mismatch
- bad or outdated import related to `files`

Treat these as starting points, not stopping points.
After fixing known issues, keep rebuilding and fix any newly surfaced blockers until the APK is produced.

---

## How to fix code

When a symbol is broken, first search the repository for the real current definition.

Search examples:

- `volumeKeysPaging`
- `ProgressRepository`
- `getProgress`
- `saveProgress`
- `files`

Then fix the call sites to match the current codebase reality.

If a field was renamed:
- update repository, mapper, constructor, and related call sites consistently

If an interface moved or changed:
- update imports
- update implemented interface names
- update method signatures to match the real interface

Avoid speculative changes.
Verify with the codebase before editing.

---

## Validation order

After each meaningful fix:

```bash
./gradlew :data-local:compileDebugKotlin --stacktrace
```

Once that passes:

```bash
./gradlew assembleDebug --stacktrace
```

If more errors appear, continue fixing them in sequence.

Only stop when one of these is true:

1. the Debug APK exists
2. a true external blocker is proven, such as:
   - missing private credentials
   - required secrets not available in the environment
   - unavailable external infrastructure
   - missing proprietary dependency that cannot be recovered from the repository

If blocked externally, clearly state:
- what is blocked
- the exact evidence
- the minimal human action required

---

## Pull request expectations

When the build is restored, prepare a PR summary.

Suggested PR title:

```text
fix(android): restore debug APK build pipeline
```

PR description should include:

1. User-visible symptom
2. Root cause chain
3. Files changed
4. Why each change was needed
5. Validation commands run
6. Final APK output path
7. Any remaining risks or follow-up suggestions

Do not claim success without listing the final APK path.

---

## Output expectations for the final result

Provide:

1. Root cause summary
2. Modified file list
3. Explanation of each change
4. Validation commands executed
5. Exact APK output path
6. PR title
7. PR description
