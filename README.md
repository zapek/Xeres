[![Main site](docs/logo.png)](https://xeres.io)

https://xeres.io

## A decentralized and secure application designed for communication and sharing.

[![GitHub release](https://img.shields.io/github/release/zapek/Xeres.svg?label=latest%20release)](https://github.com/zapek/Xeres/releases/latest)
[![CodeQL](https://github.com/zapek/Xeres/actions/workflows/analysis.yml/badge.svg)](https://github.com/zapek/Xeres/actions/workflows/analysis.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=zapek_Xeres&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=zapek_Xeres)
[![License](https://img.shields.io/github/license/zapek/Xeres.svg)](https://github.com/zapek/Xeres/blob/master/LICENSE)

## Supported platforms

- Windows (x86_64)
- Linux (x86_64)
- MacOS (x86_64)

## Downloads

Latest release available [here](https://github.com/zapek/Xeres/releases/latest).

## Features

- Peer-to-Peer (Friend-to-Friend), decentralized
- Fully compatible with [Retroshare](https://retroshare.cc) 0.6.6 or higher
- Hardware accelerated encryption ([AES-NI](https://en.wikipedia.org/wiki/AES_instruction_set)) support
- [JavaFX](https://openjfx.io/) UI
- High concurrency

## Documentation and support
- [User documentation & FAQ](https://xeres.io/docs/)
- [Wiki and technical documentation](https://github.com/zapek/Xeres/wiki)
- [Roadmap](https://github.com/users/zapek/projects/4)
- [Discussions & Forums](https://github.com/zapek/Xeres/discussions)
- [Issues reporting](https://github.com/zapek/Xeres/issues)

## Donations

Please consider a donation to help with the project's development. Contact me to get listed in the application's about window. The more donations, the more time is allocated on the project.

| Method | Address                                                                                         |
|--------|-------------------------------------------------------------------------------------------------|
| GitHub | https://github.com/sponsors/zapek                                                               |
| PayPal | https://www.paypal.me/zapek666                                                                  |
| BTC    | bc1qn57zvp8s3h6renf805fan53kt7q4j963g7prvt                                                      |
| XMR    | 84czz4Vg44GaGmQF8Lst3uWEMyXLSahBffhrDd77jdVJEoKCtUJF96mGQ4XzcYrLG1JGaj2hr2sMoDoihQ52MT1jMBnucyu |
| GOST   | GM72AdtcAKLT8DGHSgDGeTA8Zsub23wL4K                                                              |
| ETH    | 0x7d9EfEe706c81227c73DA7814319301C6Bd63D05                                                      |
| ZEN    | znePxvhiUQLp7arEEVvuC1rkofgJf3LZ2uw                                                             |
| BAT    | https://github.com/zapek/Xeres/commits?author=zapek (use tip button in Brave browser)           |

## Build requirements

- Java 8 or higher so that Gradle can bootstrap the rest (Xeres itself uses Java 19)

If you want to quickly try the current development version without installing anything else, see the [command line](#Command-line) section below.

## How to run

##### IntelliJ IDEA Ultimate

It is recommended to run the _XeresApplication_ Spring Boot configuration which is the most convenient and fastest way.  
Just make sure to configure it in the following way:

Select _Edit Configurations..._ of the _XeresApplication_ Spring Boot configuration.

Put the following _VM options_:

    -ea -Djava.net.preferIPv4Stack=true

And the following _Active profiles_:

    dev

Optionally, for faster build/test turnarounds you can add in the _program arguments_:

	--fast-shutdown

Then just run the _XeresApplication_ Spring Boot configuration.

##### IntelliJ Community Edition

Run the Gradle ``bootRun`` target. It's in the top right _Gradle_ panel, Tasks / application. It's already preconfigured.

(This way also works with IntelliJ IDEA Ultimate, but you'll miss some extras like colored debug output and faster launch)

##### Command line

###### Windows

	gradlew.bat bootRun

###### Linux

	./gradlew bootRun

To pass Xeres arguments, just use the args feature, for example:

	./gradlew bootRun --args="--no-gui --fast-shutdown"

(Use ``--help`` to know all arguments)

## Database debugging

With IntelliJ Ultimate, create the following Database connection with the built-in Datagrip client (aka the _Database_ tool window)

- Connection type: Embedded
- Driver: H2
- Path: select ``./data/userdata.mv.db``. If the file is not there, run Xeres once.
- Authentication: User & Password
- User: ``sa``
- There's no password

## Useful Gradle tasks

##### Running tests locally

run the ``test`` task

##### Cleaning the build directory

run the ``clean`` task

##### Upgrading Gradle

- change the version in _build.gradle_ in the _wrapper_ section
- run the ``wrapper`` task

## Git branching model

*master* always contains the current and runnable code. Use a feature branch to work on a feature (for example, feature/165-the-feature (165 would be the ticket number, if any)). Once it's ready, have someone review it then merge to master.

Releases use tags and might use a release branches if urgent fixes are needed.

More information: [Git process that works - say no to GitFlow](https://reallifeprogramming.com/git-process-that-works-say-no-to-gitflow-50bf2038ccf7).

## Manual testing

##### Using multiple configs

Pass the following arguments to run multiple instances.

Location A:

	--data-dir=./data2 --control-port=1068

Location B:

	--data-dir=./data3 --control-port=1069
