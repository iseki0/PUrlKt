---
name: purlkt-release
description: Execute and verify the PurlKt release workflow for publishing a new version to Maven Central via GitHub Actions. Use when asked to cut a release, prepare or validate a version tag, troubleshoot release pipeline failures, or keep README dependency versions aligned with the latest GitHub release.
---

# Purlkt Release

Use this skill to run a safe, repeatable release for `purlkt`.

## Inputs

Collect these before action:
- Target version (for example `0.0.8`)
- Whether to create and push tag in this run
- Whether to create GitHub Release in this run

Use tag format `v<version>` (for example `v0.0.8`).

## Workflow

1. Validate repository state.
- Run `git status --short`.
- Stop if unrelated dirty changes exist and confirm scope with user.

2. Confirm release baseline.
- Read [release-workflow.md](references/release-workflow.md).
- Verify release triggers:
  - `.github/workflows/build.yml` publishes on `refs/tags/v*`.
  - `.github/workflows/update-version.yml` updates `README.md` on GitHub Release `published`.

3. Run pre-release checks locally.
- Run `./gradlew build check`.
- If build fails with `:kotlinStoreYarnLock` and `Lock file was changed`, run `./gradlew kotlinUpgradeYarnLock` then `./gradlew build`.
- If build fails with `:kotlinWasmStoreYarnLock` and `Lock file was changed`, run `./gradlew kotlinWasmUpgradeYarnLock` then `./gradlew build`.
- Commit lockfile updates only when they are expected.

4. Prepare and push tag.
- Confirm target tag does not exist locally/remotely.
- Create annotated tag `v<version>`.
- Push branch and tag.

5. Monitor release pipeline.
- Watch `build` workflow for tag run.
- Confirm `publishAndReleaseToMavenCentral` step succeeds.

6. Publish GitHub Release (required for README sync).
- Verify GitHub CLI auth first: `gh auth status`.
- Create or publish release for the same tag `v<version>` (for example `gh release create v0.0.8 --title v0.0.8 --generate-notes`).
- Ensure release is `published` (not draft), because `.github/workflows/update-version.yml` listens to `release.published`.

7. Finalize README sync.
- Confirm `update-version` workflow updates `README.md` version snippets.

8. Verify release result.
- Check artifact availability in Maven Central.
- Check repo tip after README auto-update.

## Guardrails

- Do not change workflow triggers unless asked.
- Do not bypass signing for commits in this repository.
- Prefer absolute evidence: workflow run URL, tag name, commit SHA, artifact version.

## References

- [release-workflow.md](references/release-workflow.md): commands, failure playbook, and verification checklist.
