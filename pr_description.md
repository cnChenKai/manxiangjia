## Root cause summary
The build pipeline was failing due to several independent issues:
1. Missing `volumeKeysPaging` field in `ItemSettingsEntity` and `ItemReaderSettingsOverride`.
2. A circular dependency introduced when trying to make `data-local` use `ProgressRepository` from `data-files`.
3. Java compilation failures from Hilt code generation because `com.mangahaven.data.files.import` uses the reserved keyword `import` in Java.
4. Various minor API breaking changes and incorrect context injection bindings, such as using `ImageFileUtils.isIgnoredEntry` instead of `shouldIgnore`, and missing `OkHttpClient` dependency in DI.
5. Inconsistent usage of Kotlin JVM compiler options compared to Java versions.
6. The CI workflow executing `gradle` instead of the required `./gradlew`.

## Modified file list
* `app/build.gradle.kts`
* `core-model/build.gradle.kts`
* `core-model/src/main/kotlin/com/mangahaven/model/repository/ProgressRepository.kt`
* `data-files/src/main/kotlin/com/mangahaven/data/files/importer/...` (Moved from `.import`)
* `data-files/src/main/kotlin/com/mangahaven/data/files/di/FilesModule.kt`
* `data-local/src/main/kotlin/com/mangahaven/data/local/entity/ItemSettingsEntity.kt`
* `data-local/src/main/kotlin/com/mangahaven/data/local/repository/ItemSettingsRepository.kt`
* `feature-reader/src/main/kotlin/com/mangahaven/feature/reader/PageImageLoader.kt`
* `feature-reader/src/main/kotlin/com/mangahaven/feature/reader/ReaderScreen.kt`
* `feature-reader/src/main/kotlin/com/mangahaven/feature/reader/ReaderViewModel.kt`
* `.github/workflows/android.yml`
* `gradlew` / `gradle/wrapper/gradle-wrapper.jar`

## Why each file changed
* `app/build.gradle.kts`: Enabled `buildConfig` feature so `BuildConfig` generation succeeds.
* `core-model/build.gradle.kts`: Standardized the `jvmTarget` for `KotlinCompile` to 17 to match Java.
* `ProgressRepository.kt`: Moved to `core-model/src/main/kotlin/com/mangahaven/model/repository` to fix circular module dependencies between `data-local` and `data-files`.
* `.importer` files: Renamed package from `.import` to `.importer` to avoid Java reserved keyword compilation failure in Dagger Hilt generation.
* `FilesModule.kt`: Added `OkHttpClient` `@Provides` binding to resolve Hilt missing dependency error.
* `ItemSettingsEntity.kt` & `ItemSettingsRepository.kt`: Re-added `volumeKeysPaging` field to resolve `Unresolved reference` errors.
* `PageImageLoader.kt`: Converted `loadSinglePageBitmap` to a `suspend` function since it is running within `withContext(Dispatchers.IO)`.
* `ReaderScreen.kt` & `ReaderViewModel.kt`: Added missing imports and constructor parameters.
* `android.yml`: Replaced `gradle` with `./gradlew` to ensure the correct wrapper configuration runs on CI.
* `gradlew`: Restored missing wrapper scripts.

## Validation commands executed
```bash
./gradlew :app:assembleDebug --stacktrace
```

## Exact APK output path
```
app/build/outputs/apk/debug/app-debug.apk
```
