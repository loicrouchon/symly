# Symly

Symly is a tool helping to replicate and maintain the file structure of a `repository` into a
directory `source directory` by creating symbolic links.

For example, consider the following directory `~/my/repository`:

```txt
~/my/repository
 |-- .bashrc
 |-- .gitconfig
 >-- .config
     |-- starship.toml
     >-- fish
         |-- config.fish
```

By using symly, you can automatically create links in the user home directory (`~`) to all files in `~/my/repository`.

```cmd
> symly link --to ~/my/repository
```

This would result in the following links:

```txt
~/.bashrc                   ->  ~/my/repository/.bashrc
~/.gitconfig                ->  ~/my/repository/.gitconfig
~/.config/starship.toml     ->  ~/my/repository/.config/starship.toml
~/.config/fish/config.fish  ->  ~/my/repository/.config/fish/config.fish
```

## What is symly useful for?

The Symly tool has been created to address the synchronization issue of user's configuration files between different
machines. But Symly is not a synchronization tool, nor is limited to deal with user configuration files.

> *Symly is not a synchronization tool.*

### There are many synchronization tools out there, how is Symly different?

Symly is not a synchronization tool. Symly is a tool allowing you to centralize a list of files and directories and to
create symbolic links to them. You can then use your preferred synchronization tool whether it is a service like
Dropbox, Tresorit, or a more developer and versioning-oriented tool like git.

### Why creating symbolic links and not copy the files over?

The main use case of symly is about user configuration files. The particularity of those files is that they are not
read-only. They are written to by a wide variety of tools like your preferred text-editor, scripts appending to
your `.bashrc`, other tools like `git config --global`, ... Using a file copy would destroy such updates every time you
want to deploy your centralized files.

* **Seamless integration**:
  by using symbolic links every change to tracked files will be immediately and available in the repository, no matter
  the tool used to edit the files.
* **Synchronization/Versioning ready**:
  Any versioning or synchronization tool can be used on the repository. Gain instant benefit from your favorite tool
  diffs and rollbacks for your files.

### Is Symly limited to user configuration files?

No, Symly is not limited to creating links in your user home folder. The user home folder is a sensible default for
the `main directory` but any other `main directory` can be used.

## Installation

Symly is available through deb and rpm package managers for linux

**DEB**: Debian, Ubuntu, Linux Mint, ...

```cmd
sudo sh -c 'curl -1sLf https://packages.loicrouchon.fr/deb/dists/latest/Release.gpg.key | gpg --dearmor > /etc/apt/trusted.gpg.d/loicrouchon-packages.gpg'
sudo sh -c 'echo "deb [arch=amd64] https://packages.loicrouchon.fr/deb latest main" > /etc/apt/sources.list.d/symly.list'
sudo apt update
sudo apt install symly
```

**RPM**: Fedora, CentOS, Red Hat, ...

```cmd
sudo sh -c 'curl -1sLf https://packages.loicrouchon.fr/rpm/Release.gpg.key > /tmp/symly.gpg.key'
sudo rpm --import /tmp/symly.gpg.key
sudo dnf install 'dnf-command(config-manager)'
sudo dnf config-manager --add-repo https://packages.loicrouchon.fr/rpm
sudo dnf install symly
```

It is also available via HomeBrew for macOS and other linux distributions.

**Homebrew** (MacOS/Linux)

```cmd
brew tap loicrouchon/symly
brew install symly
```

## Manual download

Artifacts can also be downloaded manually from [github](https://github.com/loicrouchon/symly/tags).

The following artifacts are available:

* Jar application with bootstrap script. Requires JVM 11+
* Native binaries for Linux (x64) and macOS (x64)
* `.deb` and `.rpm` packages.

## Usage

```cmd
Usage: symly [-hv] [COMMAND]
symly create links
-h, --help      Prints this help message and exits
-v, --verbose   Be verbose.
Commands:
link        Synchronizes the links from the repositories to the source directory
```

### The `link` subcommand

The `link` subcommand is the main subcommand that will perform the linking.

```txt
Usage: symly link [--dry-run] [-d=<main-directory>] -t=<repositories>...
                  [-t=<repositories>...]...
Synchronizes the links from the target directories to the destination
  -d, --dir, --directory=<main-directory>
                  Main directory in which links will be created
                    Default: /Users/loicrouchon
      --dry-run   Do not actually create links but only displays which ones
                    would be created
  -t, --to=<repositories>...
                  Target directories (a.k.a. repositories) containing files to
                    link in the main directory
```

* Links every file from the `repositories` into the `main directory` by preserving the structure.

```txt
> tree ~/my/repository
~/my/repository
 |-- .bashrc
 |-- .gitconfig
 >-- .config
     |-- starship.toml
     >-- fish
         |-- config.fish

> symly link --to ~/my/repository

Creating links in ~ to [~/my/repository]
[CREATE] ~/.bashrc -> ~/my/repository/.bashrc
[CREATE] ~/.gitconfig -> ~/my/repository/.gitconfig
[CREATE] ~/.config/starship.toml -> ~/my/repository/.config/starship.toml
[CREATE] ~/.config/fish/config.fish -> ~/my/repository/.config/fish/config.fish
```  

* Supports multiple `repositories`: The first repository defining a link has the priority, defaults are last

```txt
> tree ~/my/repositories
~/my/repositories
 |-- custom
 |   |-- .gitconfig
 |   >-- .bashrc
 >-- defaults
     |-- .gitconfig
     >-- .config
        >-- starship.toml

> symly link --to ~/my/repositories/custom ~/my/repositories/defaults

Creating links in ~ to [~/my/repositories/custom, ~/my/repositories/defaults]
[CREATE] ~/.bashrc -> ~/my/repositories/custom/.bashrc
[CREATE] ~/.gitconfig -> ~/my/repositories/custom/.gitconfig
[CREATE] ~/.config/starship.toml -> ~/my/repositories/defaults/.config/starship.toml
```  

* Supports directory linking when a `.symlink` file is present in the directory.

```txt
> tree ~/my/repository
~/my/repository
 >-- .config
     >-- fish
         |-- .symlink
         >-- config.fish

> symly link --to ~/my/repository

Creating links in ~ to [~/my/repository]
[CREATE] ~/.config/fish -> ~/my/repository/.config/fish
```  

## Terminology

The terminology examples will be given by considering the following file structure:

```txt
~
 |-- .gitconfig         -> ~/repositories/custom/.gitconfig
 |-- .bashrc            -> ~/repositories/custom/.bashrc
 >-- .config
     >-- starship.toml  -> ~/repositories/defaults/config/starship.toml

~/repositories
 |-- custom
 |   |-- .gitconfig
 |   >-- .bashrc
 >-- defaults
     |-- .gitconfig
     >-- .config
        >-- starship.toml
```

**Main directory**:
A directory in which a `repository`  file structure will be linked.

Example: `~`

**Repository**:
A directory containing a file structure to link into a `main directory`.

Example: `~/repositories/custom`, `~/repositories/defaults`

**Link**:

A link is composed of two attributes:

* **source**: The path of the link.
* **target**: The path pointed by the link.

It is materialized on the file system as a [symbolic link](https://en.wikipedia.org/wiki/Symbolic_link).

For example: `~/.config/starship.toml -> ~/repositories/defaults/.config/starship.toml`

Those two attributes can be determined from the _main directory, and the repository_ using a third one: the _link
name_
.

The **link name** is the common part of the path between the link source and target. It is both:

* The relative path of a link source to its main directory.
* The relative path of a link target to its repository.

Example:

* `.gitconfig` for link `~/.gitconfig -> ~/repositories/custom/.gitconfig`
* `.config/starship.toml` for link `~/.config/starship.toml -> ~/repositories/defaults/.config/starship.toml`

### Summary

Here is how all the previous notions play together:

```txt
 ~               /  .config/starship.toml  ->  ~/repositories/defaults  /  .config/starship.toml
[MAIN DIRECTORY] / [NAME                 ] -> [REPOSITORY             ] / [NAME                 ]
[SOURCE                                  ] -> [TARGET                                           ]
```

## Build

You can also clone this repository and build Symly using the instructions below:

### Pre-requisites

* JDK 11

### Packaging and running the application

The application can be built using the `installDist` task:

```shell script
./gradlew clean build installDist
```

This will install the application locally in the `./build/install/symly/`. The application can be run
using `./build/install/symly/bin/symly <ARGS>` or using `./build/install/symly/bin/symly.bat` on Windows.

```txt
> ./build/install/symly/bin/symly

Usage: symly [-hv] [COMMAND]
symly create links
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

* `build/distributions/symly-${version}.tar`
* `build/distributions/symly-${version}.zip`

Once unzipped/untarred, the application can be run using the same `bin/link`/`bin/symly.bat` launch script as above.

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

You can then execute your native executable with: `./build/libs/symly <ARGS>`
