# Updating Java version

First, consider sticking to LTS unless there's a very desired feature or an important bug fix. There's often a gap between _no support_ of a non-LTS version and Gradle not supporting the newer one yet.

- make sure there's an OpenJDK release (https://openjdk.org/)
- make sure IntelliJ supports it (https://www.jetbrains.com/help/idea/supported-java-versions.html although this page is often wrong)
- make sure Gradle supports it in its toolchain mode (its normal mode is hopeless since they started adding Kotlin, also Kotlin frequently lags behind JDK releases)
- download the latest _Oracle OpenJDK_ release in IntelliJ
- switch the project SDK to it
- change the java toolchain version in the root build.gradle
- run all tests and make a dry run
- change the java version in .github/workflows
- change the java references in the root README
