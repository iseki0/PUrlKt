# PurlKt Release Workflow Reference

## Source of Truth Files

- `build.gradle.kts`
- `.github/workflows/build.yml`
- `.github/workflows/update-version.yml`
- `README.md`
- `AGENTS.md`

## Commands

Run from repository root.

```powershell
# 1) Ensure clean state
git status --short

# 2) Optional: inspect recent tags
git tag --list --sort=-creatordate

# 3) Pre-release validation
./gradlew build check
```

If lockfile validation fails:

```powershell
./gradlew kotlinUpgradeYarnLock
./gradlew build
```

Or for wasm lockfile:

```powershell
./gradlew kotlinWasmUpgradeYarnLock
./gradlew build
```

Create and push release tag:

```powershell
$version = "0.0.8"
$tag = "v$version"
git tag -a $tag -m "release: $tag"
git push origin master
git push origin $tag
```

GitHub release (required to trigger README auto-update workflow):

```powershell
gh auth status
gh release view $tag --json tagName,isDraft,isPrerelease,publishedAt,url
# if release does not exist
gh release create $tag --title $tag --generate-notes
```

## Trigger Logic

- Push tag `v*` triggers `build.yml`.
- On tag runs, `build.yml` publishes with:
  - `./gradlew publishAndReleaseToMavenCentral -Pversion=${VERSION:1}`
- GitHub Release event `published` triggers `update-version.yml`.
- `update-version.yml` updates `README.md` dependency versions and pushes commit to `master`.

## Verification Checklist

After tag push:

1. `build` workflow is running for `refs/tags/vX.Y.Z`.
2. `Publish to Maven Central(Tagged)` step succeeded.
3. Artifact `space.iseki.purlkt:purlkt:X.Y.Z` appears in Maven Central.

After GitHub Release published:

1. `Update Version in README` workflow completed.
2. `README.md` contains new version in Gradle and Maven snippets.
3. Auto-commit from `github-actions[bot]` is present on `master`.

## Failure Playbook

- Lockfile changed during build:
  - Run upgrade task (`kotlinUpgradeYarnLock` or `kotlinWasmUpgradeYarnLock`), commit result, rerun build.
- Tag already exists:
  - Stop and confirm whether to bump version or re-release.
- Publish step fails on credentials:
  - Check `OSSRH_USERNAME` and `OSSRH_PASSWORD` secrets in GitHub Actions.
- Publish step fails on signing:
  - Check imported GPG key in workflow secret `GPG_PK`.
- README not updated:
  - Confirm GitHub Release was published (tag push alone is not enough).
  - Confirm `gh auth status` is valid before creating release from CLI.
