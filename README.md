# Linky

## Pre-requisites

* JDK 11

## Packaging and running the application

The application can be built using the `installDist` task:

```shell script
./gradlew clean installDist
```

This will install the application locally in the `./build/install/linky/`. The application can be run
using `./build/install/linky/bin/linky <ARGS>` or using `./build/install/linky/bin/linky.bat` on windows.

```
$ ./build/install/linky/bin/linky

Usage: linky [-hv] [COMMAND]
linky create links
  -h, --help      Prints this help message and exits
  -v, --verbose   Be verbose.
Commands:
  link  link
```

## Advanced packaging options

### Creating a distribution

The `assembleDist` task allow to build a `tar` and a `zip` distribution archives:

```shell script
./gradlew clean assembleDist
```

This result in two self-contained archive which only requires a JRE to be installed resulting. The archives are located
here:

* `build/distributions/linky-${version}.tar`
* `build/distributions/linky-${version}.zip`

Once unzipped/untarred, the application can be run using the same `bin/link`/`bin/linky.bat` launch script as above.

### Creating a native executable

#### Prerequisites

* You must have GraalVM installed in its version `${graalvm.version}` (see `pom.xml` for the exact version).
* The `GRAALVM_HOME` must point to GraalVM installation directory.
* `$PATH` must include `$GRAALVM_HOME/bin`
* `native-image` must have been installed using `gu install native-image`

#### Building the native executable

You can create a native executable using the `buildNativeImage` task:

```shell script
./gradlew clean buildNativeImage
```

You can then execute your native executable with: `./build/libs/linky <ARGS>`
