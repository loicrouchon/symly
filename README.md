# Linky

Linky is a tool helping to replicate and maintain the file structure of a directory `source` into a directory `destination` by creating symbolic links.

For example, consider the following directory `~/my/source`:
```
~/my/source
 |-- .bashrc
 |-- .gitconfig
 >-- .config
     |-- starship.toml
     >-- fish
         |-- config.fish
```

By using linky, you can automatically create links from `~` to all files in `~/my/source`.

```
> linky link -s ~/my/source
```

This would result in the following links:
```
~/.bashrc                   ->  ~/my/source/.bashrc
~/.gitconfig                ->  ~/my/source/.gitconfig
~/.config/starship.toml     ->  ~/my/source/.config/starship.toml
~/.config/fish/config.fish  ->  ~/my/source/.config/fish/config.fish
```

## What is linky useful for?

The Linky tool has been created to address the synchronization issue of user's configuration files between different machines.
But Linky is not a synchronization tool, nor is limited to deal with user configuration files.

> *Linky is not a synchronization tool.*

### There are many synchronization tools out there, how is Linky different?

Linky is not a synchronization tool.
Linky is a tool allowing you to centralize a list of files and directories and to create symbolic links to them. You can then use your preferred synchronization tool whether it is a service like Dropbox, Tresorit, or a more developer and versioning-oriented tool like git.

### Why creating symbolic links and not copy the files over?

The main use case of linky is about user configuration files.
The particularity of those files is that they are not read-only.
They are written to by a wide variety of tools like your preferred text-editor, scripts appending to your `.bashrc`, other tools like `git config --global`, ...
Using a file copy would destroy such updates every time you want to deploy your centralized files.

* **Seamless integration**:
  by using symbolic links every change to tracked files will be immediately and available in the source directory, no matter the tool used to edit the files.
* **Synchronization/Versioning ready**:
  Any versioning or synchronization tool can be used on the source directory. Gain instant benefit from your favorite tool diffs and rollbacks for your files.

### Is Linky limited to user configuration files?

No, Linky is not limited to creating links in your user home folder.
The user home folder is a sensible default for the `destination` but any `destination` can be used.

## Usage

```
Usage: linky [-hv] [COMMAND]
linky create links
-h, --help      Prints this help message and exits
-v, --verbose   Be verbose.
Commands:
link        Synchronizes the links from the sources to the destination
```

### The `link` subcommand

The `link` subcommand is the main subcommand that will perform the linking.

```
Usage: linky link [--dry-run] [-d=<destination>] -s=<sources>...
```
* Links every file from `sources` into `destination` by preserving the structure.
```
> tree ~/my/source
~/my/source
 |-- .bashrc
 |-- .gitconfig
 >-- .config
     |-- starship.toml
     >-- fish
         |-- config.fish

> linky link -s ~/my/source

Creating links from [~/my/source] to ~
[CREATE] ~/.bashrc -> ~/my/source/.bashrc
[CREATE] ~/.gitconfig -> ~/my/source/.gitconfig
[CREATE] ~/.config/starship.toml -> ~/my/source/.config/starship.toml
[CREATE] ~/.config/fish/config.fish -> ~/my/source/.config/fish/config.fish
```  

* Supports multiple `sources`: Override first, defaults last
```
> tree ~/my/source
~/my/source
 |-- custom
 |   |-- .gitconfig
 |   >-- .bashrc
 >-- defaults
     |-- .gitconfig
     >-- .config
        >-- starship.toml

> linky link -s ~/my/source/custom ~/my/source/defaults

Creating links from [~/my/source/custom, ~/my/source/defaults] to ~
[CREATE] ~/.bashrc -> ~/my/source/custom/.bashrc
[CREATE] ~/.gitconfig -> ~/my/source/custom/.gitconfig
[CREATE] ~/.config/starship.toml -> ~/my/source/defaults/.config/starship.toml
```  

* Supports directory linking when a `.symlink` file is present in the directory.
```
> tree ~/my/source
~/my/source
 >-- .config
     >-- fish
         |-- .symlink
         >-- config.fish

> linky link -s ~/my/source

Creating links from [~/my/source] to ~
[CREATE] ~/.config/fish -> ~/my/source/.config/fish
```  

## Terminology

* **Destination**: a directory in which a `source`  file structure will be linked.
* **Link**: a [symbolic link](https://en.wikipedia.org/wiki/Symbolic_link)
* **Source**: a directory containing a file structure to link in a `destination`.

## Installation

Native binaries for Linux (x64) and macOS (x64) can be downloaded directly from [github](https://github.com/loicrouchon/linky/tags).
It is intended to provide linky as `.deb`, `.rpm` packages and through Homebrew in a near future.

## Build

You can also clone this source and build Linky using the instructions below:

### Pre-requisites

* JDK 11

### Packaging and running the application

The application can be built using the `installDist` task:

```shell script
./gradlew clean build installDist
```

This will install the application locally in the `./build/install/linky/`. The application can be run
using `./build/install/linky/bin/linky <ARGS>` or using `./build/install/linky/bin/linky.bat` on windows.

```
> ./build/install/linky/bin/linky

Usage: linky [-hv] [COMMAND]
linky create links
  -h, --help      Prints this help message and exits
  -v, --verbose   Be verbose.
Commands:
  link  link
```

### Advanced packaging options

### Creating a distribution

The `assembleDist` task allow to build a `tar` and a `zip` distribution archives:

```shell script
./gradlew clean build assembleDist
```

This result in two self-contained archive which only requires a JRE to be installed resulting. The archives are located
here:

* `build/distributions/linky-${version}.tar`
* `build/distributions/linky-${version}.zip`

Once unzipped/untarred, the application can be run using the same `bin/link`/`bin/linky.bat` launch script as above.

#### Creating a native package containing the JVM

##### Prerequisites
* A JDK 14 or above is required

The `buildNativePackage` task allow to build a native package installer that will contain the application as well as the JRE.

```shell script
./gradlew clean build buildNativePackage
```
The native package will be located in `build` and can be installed with regular system package manager.

#### Creating a native executable

##### Prerequisites

* You must have GraalVM installed in a version supporting Java 11.
* The `GRAALVM_HOME` must point to GraalVM installation directory.
* `$PATH` must include `$GRAALVM_HOME/bin`
* `native-image` must have been installed using `gu install native-image`

##### Building the native executable

You can create a native executable using the `buildNativeImage` task:

```shell script
./gradlew clean buildNativeImage
```

You can then execute your native executable with: `./build/libs/linky <ARGS>`
