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
