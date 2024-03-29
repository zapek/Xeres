# Updating Java version

First, consider sticking to LTS unless there's a very desired feature or an important bug fix. There's often a gap between _no support_ of a non-LTS version and Gradle not supporting the newer one yet.

- make sure there's a Temurin release (https://adoptium.net/temurin/releases/)
- make sure IntelliJ supports it (https://www.jetbrains.com/help/idea/supported-java-versions.html although this page is often wrong)
- make sure Gradle supports it (without using toolchains)
- download the latest _Temurin_ release in IntelliJ
- switch the project SDK to it
- change the sourceCompatibility in the root build.gradle
- run all tests and make a dry run
- change the java version in .github/workflows
- change the java references in the root README
