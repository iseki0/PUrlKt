# PUrlKt
[![Maven Central Version](https://img.shields.io/maven-central/v/space.iseki.purlkt/purlkt)](https://central.sonatype.com/artifact/space.iseki.purlkt/purlkt)
![License](https://img.shields.io/github/license/iseki0/PUrlKt)

A Kotlin library for parsing and generating [package-url](https://github.com/package-url/purl-spec).

## Getting Started

### Add dependency

#### Gradle

```kotlin
dependencies {
    implementation("space.iseki.purlkt:purlkt:0.0.6")
}
```

#### Maven

Since the project is in Kotlin Multiplatform, for Maven user you have to specify the platform explicitly.
(The `-jvm` suffix)

```xml
<dependency>
    <groupId>space.iseki.purlkt</groupId>
    <artifactId>purlkt-jvm</artifactId>
    <version>0.0.6</version>
</dependency>
```
