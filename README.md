[![Main site](docs/logo.png)](https://xeres.io)

[![GitHub release](https://img.shields.io/github/release/zapek/Xeres.svg?label=latest%20release)](https://github.com/zapek/Xeres/releases/latest)
[![License](https://img.shields.io/github/license/zapek/Xeres.svg)](https://github.com/zapek/Xeres/blob/master/LICENSE)
[![CodeQL](https://github.com/zapek/Xeres/actions/workflows/analysis.yml/badge.svg)](https://github.com/zapek/Xeres/actions/workflows/analysis.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zapek_Xeres&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=zapek_Xeres)

[Xeres](https://xeres.io) is a decentralized and secure application designed for communication and sharing.

---

![Xeres Desktop](docs/screenshot-chat.jpg)

## Table of Contents

- [Features](#features)
- [Supported Platforms](#supported-platforms)
- [Releases](#releases)
- [Getting Help](#getting-help)
- [Documentation](#documentation)
- [Development](#development)
- [Donations](#donations)

## Features

Xeres is an application that allows to connect to other peers to exchange information.

- 🤝 Peer-to-Peer (Friend-to-Friend), fully decentralized
- 🚫 No censorship. Cannot be censored
- 👋 Compatible with [Retroshare](https://retroshare.cc) 0.6.6 or higher
- 🛠️ Hardware accelerated encryption
- 🖥️ Desktop User Interface
- 📶 Remote access
- 🚀 Asynchronous design
- ✈️ High concurrency
- 📖 Free software (GPL)

## Supported Platforms

- Windows (x86_64)
- Linux (x86_64)
- MacOS (x86_64)

## Releases

Latest release always available [here](https://github.com/zapek/Xeres/releases/latest).

## Getting Help

- [User Documentation & FAQ](https://xeres.io/docs/)
- [Discussions & Forums](https://github.com/zapek/Xeres/discussions)
- [Issues Reporting](https://github.com/zapek/Xeres/issues)

## Documentation

- [Technical Documentation](https://github.com/zapek/Xeres/wiki)
- [Roadmap](https://github.com/users/zapek/projects/4)

## Development

### Build Requirements

- Java 21

If you want to quickly try the current development version without installing anything else, see the [command line](#command-line) section below.

### How to run

##### IntelliJ IDEA Ultimate

Run the _XeresApplication_ Spring Boot configuration.

- Active Profile: `dev`
- VM Options: `-ea -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8`
- CLI arguments: `--fast-shutdown`

##### IntelliJ Community Edition

Run the Gradle ``bootRun`` target. It's in the top right _Gradle_ panel, Tasks / application. It's already preconfigured.

(This way also works with IntelliJ IDEA Ultimate, but you'll miss some extras like colored debug output and faster launch)

##### Command Line

###### Windows

	gradlew.bat bootRun

###### Linux

	./gradlew bootRun

To pass arguments to Xeres, just use the args feature, for example:

	./gradlew bootRun --args="--no-gui --fast-shutdown"

(Use ``--help`` to know all arguments)

### Database Debugging

#### Online

When running Xeres with the `dev` profile, there's a built-in H2 Console available, accessible through the _Debug_ menu.

#### Offline

With IntelliJ Ultimate, create the following Database connection with the built-in Datagrip client (aka the _Database_ tool window)

- Connection type: Embedded
- Driver: H2
- Path: select ``./data/userdata.mv.db``. If the file is not there, run Xeres once.
- Authentication: User & Password
- User: ``sa``
- There's no password

You can also download the [H2 installer](https://www.h2database.com/html/download.html) (version to use: 2.1.214).
Then run the H2 console with the following settings:

- Saved Settings: Generic H2 (Embedded)
- Driver Class: org.h2.Driver
- JDBC URL: `hdbc:h2:file:~/workspace/Xeres/data/userdata` (put your path, and no file extension at the end!)
- User Name: sa
- The password is empty

### Useful Gradle Tasks

##### Running Tests Locally

run the ``test`` task

##### Cleaning the Build Directory

run the ``clean`` task

##### Upgrading Gradle

- change the version in _build.gradle_ in the _wrapper_ section
- run the ``wrapper`` task

### Useful Links

##### Pick a FontAwesome Icon

https://fontawesome.com/v4/icons/

Replace all '-' by '_' in their names for use in JavaFX.

### Git Branching Model

*master* always contains the current and runnable code. Use a feature branch to work on a feature (for example, feature/165-the-feature (165 would be the ticket number, if any)). Once it's ready, have someone review it then merge to master.

Releases use tags and might use a release branches if urgent fixes are needed.

More information: [Git process that works - say no to GitFlow](https://reallifeprogramming.com/git-process-that-works-say-no-to-gitflow-50bf2038ccf7).

### Manual Testing

##### Using Multiple Configs

Pass the following arguments to run multiple instances.

Location A:

	--data-dir=./data2

Location B:

	--data-dir=./data3

### Monitoring

When running Xeres with the `dev` profile, JMX monitoring is available either using JConsole or [VisualVM](https://visualvm.github.io/).
Simply run them and connect to the Xeres session.
	
## Donations

Please consider a donation to help with the project's development. Contact me if you want to get listed in the application's about window. The more donations, the more time is allocated on the project.

| Method | Address                                                                                         |
|--------|-------------------------------------------------------------------------------------------------|
| GitHub | https://github.com/sponsors/zapek                                                               |
| PayPal | https://www.paypal.me/zapek666                                                                  |
| Coffee | https://www.buymeacoffee.com/zapek                                                              |
| BTC    | bc1qn57zvp8s3h6renf805fan53kt7q4j963g7prvt                                                      |
| XMR    | 84czz4Vg44GaGmQF8Lst3uWEMyXLSahBffhrDd77jdVJEoKCtUJF96mGQ4XzcYrLG1JGaj2hr2sMoDoihQ52MT1jMBnucyu |
| GOST   | GM72AdtcAKLT8DGHSgDGeTA8Zsub23wL4K                                                              |
| ZEN    | znePxvhiUQLp7arEEVvuC1rkofgJf3LZ2uw                                                             |
| BAT    | https://github.com/zapek/Xeres/commits?author=zapek (use tip button in Brave browser)           |

Thank you!
