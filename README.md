# Xeres

This is an attempted reimplementation of [Retroshare](https://retroshare.cc) in Java.

## Supported platforms

- Windows (x86_64)
- Linux (x86_64)
- MacOS (x86_64) _untested_

## Build requirements

- Java 17

## Features

- [AES-NI](https://en.wikipedia.org/wiki/AES_instruction_set) support
- [JavaFX](https://openjfx.io/) UI
- Web UI
- High concurrency

## Download

https://xeres.io

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

(This way also works with IntelliJ IDEA Ultimate but you'll miss some extras like colored debug output and faster launch)

##### Command line

###### Windows

	gradlew.bat

###### Linux

	./gradlew

To pass Xeres arguments, just use the args feature, ie.

	./gradlew bootRun --args="--no-gui --fast-shutdown"

(Use ``--help`` to know all arguments)

## How to setup the WebUI

_Note: the webui is currently nonfunctional._

Run the gradle tasks ``installAngular`` (if you don't already have Angular installed) then ``buildAngular``. The later will create the needed files that will be served by Xeres on ``localhost:1066``.

## Database debugging

With IntelliJ Ultimate, create the following Database connection with the built-in Datagrip client (aka the _Database_ tool window)

- Connection type: Embedded
- Driver: H2
- Path: select ``./data/userdata.mv.db``. If the file is not there, run Xeres once.
- Authentication: User & Password
- User: ``sa``
- There's no password

## Misc

The project was started on 2019-10-30.

##### How to write proper git commit messages

https://chris.beams.io/posts/git-commit/

##### Branching model

The current plan is to use *master* for everything. Use a feature branch to work on a feature (ie. feature/165 if there's a ticket). Once it's ready, have someone review it then merge to master.

Releases will use tags and release branches if further fixes are needed.

https://reallifeprogramming.com/git-process-that-works-say-no-to-gitflow-50bf2038ccf7

## Useful tasks

##### Cleaning the build directory

run the ``clean`` task

##### Cleaning the Angular generated directory

run the ``cleanAngular`` task

##### Upgrading Gradle

- change the version in _build.gradle_ in the _wrapper_ section
- run the ``wrapper`` task
