# AI Context Notes

## Build Notes

- This project is Kotlin Multiplatform and `./gradlew build` validates generated Yarn lockfiles.
- If build fails with `:kotlinStoreYarnLock` and message `Lock file was changed`, run:
  - `./gradlew kotlinUpgradeYarnLock`
- If build fails with `:kotlinWasmStoreYarnLock` and message `Lock file was changed`, run:
  - `./gradlew kotlinWasmUpgradeYarnLock`
- After running the upgrade tasks above, re-run:
  - `./gradlew build`

## Lockfile Behavior

- `kotlinWasmUpgradeYarnLock` may delete `kotlin-js-store/wasm/yarn.lock` when there are no wasm npm dependencies (this is expected behavior).

## Commit Signing

- Do not use `--no-gpg-sign` for commits in this repository.
- If commit signing fails due to sandbox or keyring access, rerun commit with escalated permissions.

## Commit Message Style

- Use a short imperative subject line in English, for example `Normalize CI ABI dump targets`.
- Prefer sentence-style subjects without a trailing period.
- Only use a prefix such as `docs:` or `chore:` when it adds clear value and matches existing history.
