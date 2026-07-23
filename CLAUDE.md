# Repo notes

## Build

Requires JDK 21 (`build.gradle` sets `sourceCompatibility`/`targetCompatibility` to 21;
CI uses Temurin 21). Build and test with:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean test
```

A JDK 17 build fails with `error: invalid source release: 21`.
