# Symly

[![Build](https://img.shields.io/github/workflow/status/loicrouchon/symly/Build?logo=GitHub&style=for-the-badge)](https://github.com/loicrouchon/symly/actions?query=workflow%3A%22Build%22)
[![CodeQL](https://img.shields.io/github/workflow/status/loicrouchon/symly/CodeQL?logo=GitHub&style=for-the-badge&label=CodeQL)](https://github.com/loicrouchon/symly/actions?query=workflow%3A%22CodeQL%22)
[![License](https://img.shields.io/github/license/loicrouchon/symly?style=for-the-badge&logo=apache)](https://github.com/loicrouchon/symly/blob/main/LICENSE)
[![Code Size](https://img.shields.io/github/languages/code-size/loicrouchon/symly?logo=java&style=for-the-badge)](https://github.com/loicrouchon/symly/archive/refs/heads/main.zip)

Symly is a tool helping to centralize sparse file trees. It replicates and maintains a file tree structure of one or
multiple `repository` layers into a `directory` by creating symbolic links.

##### Table of Contents

* [What is symly useful for?](#what-is-symly-useful-for)
* [Concepts](#concepts)
* [Features](#features)
* [Usage](#usage)
* [Installation](#installation)
* [Build](#build)

## What is symly useful for?

Symly has been created to address the synchronization issue of user's configuration files between different machines.
But Symly is not a synchronization tool, nor is limited to deal with user configuration files also known as
_dotfiles_.

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

No, Symly is not limited to creating links in your user home folder. The user home folder is however a common use case
for the directory but any other directory can be used.

## Concepts

Symly is based on the following major concepts:

* The _**repositories**_: Repositories are folders containing a file structured to be linked in a _directory_. For
  example `/some/path`, `~/repositories/defaults`. **You can specify one or more repositories**. Each repository acts
  as its own layer, overriding previous layers with its content.
* The _**directory**_: The folder in which the _repositories_ content will be linked. For example `~`.

Consider the following repository file structure:

```txt
~/repository
  |-- .gitconfig
  >-- .config
      >-- starship.toml
```

When linked with symly in the `~` directory, the following links will be created:

```txt
~/.gitconfig             -> ~/repository/.gitconfig
~/.config/starship.toml  -> ~/repository/config/starship.toml
```

_Links_ are composed of two attributes:

* **source**: The path of the link.
* **target**: The path pointed by the link.

They are materialized on the file system as [symbolic links](https://en.wikipedia.org/wiki/Symbolic_link).

For example: `~/.config/starship.toml -> ~/repository/.config/starship.toml`

Those two attributes can be determined from the _directory_, and the _repository_ using a third one: the _link name_.

The _link name_ is the common part of the path between the link source and target. It is both:

* The relative path of a link source to its main directory.
* The relative path of a link target to its repository.

Example:

* `.gitconfig` for link `~/.gitconfig -> ~/repository/.gitconfig`
* `.config/starship.toml` for link `~/.config/starship.toml -> ~/repository/.config/starship.toml`

### Summary

Here is how all the previous notions play together:

```txt
 ~          /  .config/starship.toml  ->  ~/repositories/defaults  /  .config/starship.toml
[DIRECTORY] / [NAME                 ] -> [REPOSITORY             ] / [NAME                 ]
[SOURCE                             ] -> [TARGET                                           ]
```

### The "repository file tree is the state" principle

On top of those concept, an important principle applies:

> _**The repository file tree is the state**_

This principle is has the following implications:

* No command to add a file to a repository, just drop it there
* No command to delete a file from a repository, just delete it
* No command to edit a file in a repository, just edit it directly or through its symbolic link in the directory. This
  allows for Seamless integration with tools modifying dotfiles directly on the directory (like git config user.name
  ..., …)
* Immediate visibility on modifications made on the directory files

## Features

**Commands:**

* [x] link: link the content of the repositories into the directory.
* [x] unlink: unlink the content of the repositories into the directory.
* [x] status: displays the links status

**Flexible repositories:**

* [x] Support for multiple repositories
* [x] Layering of repositories allowing for a default layer and zero to many custom ones on top.
* [x] Ignored files in repositories via .symlyignore

**Core features:**

* [x] Replication of the repository files tree structure in the directory
* [x] Symbolic link creation/update/deletion for simple files (default mode)
* [x] Symbolic link creation/update/deletion for symbolic links (default mode)
* [x] Symbolic link creation/update/deletion for directories (on demand only)
* [x] Orphan links detection/deletion
* [x] Optional forced link creation (overwriting destination file)

**Integrations:**

* [x] Seamless integration with editors, use your preferred editor or command line tools to alter your files. They don't
  even need to be aware of Symly
* [x] Seamless integration with synchronization, diff and merge tools. No need to learn a new one
* [x] Not limited to dotfiles, can be used for any other directory/file types

### Roadmap

* [ ] Command to add and link a new file to a repository
* [ ] Command to restore a file in the directory
* [ ] Command to restore a full repository

## Usage

General command help:

```cmd
Usage: symly [-hvV] [COMMAND]
symly create links
  -h, --help      Prints this help message and exits
  -v, --verbose   Be verbose.
  -V, --version   Prints version information.
Commands:
  link, ln    Create/update links from 'directory' to the 'to' repositories
  status, st  Displays the current synchronization status
  unlink      Remove links in 'directory' pointing to the 'to' repositories
```

### The `link` subcommand

The `link` subcommand is the main subcommand that will perform the linking.

```txt
Usage: symly link [-f] [--dry-run] [-d=<main-directory>]
                  [--max-depth=<max-depth>] -r=<repositories>...
                  [-r=<repositories>...]...
Create/update links from 'directory' to the 'to' repositories
  -d, --dir, --directory=<main-directory>
                  Main directory in which links will be created
                    Default: $HOME
      --dry-run   Do not actually create links but only displays which ones
                    would be created
  -f, --force     Force existing files and directories to be overwritten
                    instead of failing in case of conflicts
      --max-depth=<max-depth>
                  Depth of the lookup for orphans deletion
  -r, --repositories=<repositories>...
                  Repositories containing files to link in the main directory
```

* Links every file from the `repositories` into the `main directory` by preserving the structure.

```txt
$ tree ~/my/repository
~/my/repository
 |-- .bashrc
 |-- .gitconfig
 >-- .config
     |-- starship.toml
     >-- fish
         |-- config.fish


$ symly link --dir ~ --repositories ~/my/repository

added:      .bashrc -> ~/my/repository/.bashrc
added:      .gitconfig -> ~/my/repository/.gitconfig
added:      .config/starship.toml -> ~/my/repository/.config/starship.toml
added:      .config/fish/config.fish -> ~/my/repository/.config/fish/config.fish
```  

* Supports multiple `repositories`: Repositories are listed by increasing order of priority (default first, overrides
  last). When two repositories define a link, the link will be created to the repository with the higher priority.

```txt
$ tree ~/my/repositories
~/my/repositories
 |-- custom
 |   >-- .bashrc
 >-- defaults
     |-- .gitconfig
     >-- .config
        >-- starship.toml


$ symly link --dir ~ --repositories ~/my/repositories/defaults ~/my/repositories/custom

added:      .bashrc -> ~/my/repositories/custom/.bashrc
added:      .gitconfig -> ~/my/repositories/default/.gitconfig
added:      .config/starship.toml -> ~/my/repositories/defaults/.config/starship.toml

$ echo "Let's override the default .gitconfig and see how symly reacts"

$ touch ~/my/repositories/custom/.gitconfig

$ symly link --dir ~ --repositories ~/my/repositories/defaults ~/my/repositories/custom

deleted:    .gitconfig -> ~/my/repositories/default/.gitconfig
added:      .gitconfig -> ~/my/repositories/custom/.gitconfig
```  

* Supports directory linking when a `.symlink` file is present in the directory.

```txt
$ tree ~/my/repository
~/my/repository
 >-- .config
     >-- fish
         |-- .symlink
         >-- config.fish

$ symly link  --dir ~ --repositories ~/my/repository

added:      ~/.config/fish -> ~/my/repository/.config/fish
```

## The symly.config file

It might be annoying to keep giving the `--dir` and `--repositories` options, even when using the short names.
To this end, it is possible to define a `symly.config` file that will automatically be picked up by symly.

_For the file to be detected by symly, it needs to be present in the current working directory._

The following entries are supported:

* `directory`: the main directory.
* `repositories`: the list of repositories, separated by a `,`.
* `orphans.max-depth.search`: the max depth to look-up for orphan links.

For example:

```properties
directory=~
repositories=defaults, linux, work
```

Paths defined in this file can be:

* absolute paths.
* relative to the user directory when starting with `~`.
* relative to the file parent directory.

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

Artifacts can also be downloaded manually from [github](https://github.com/loicrouchon/symly/releases).

The following artifacts are available:

* Jar application with bootstrap script. Requires JVM 17+
* `.deb` and `.rpm` packages.

## Build

You can also clone this repository and build Symly using the instructions below:

### Pre-requisites

* JDK 17

### Packaging and running the application

The application can be built using the `installDist` task:

```shell script
./gradlew clean check installDist
```

This will install the application locally in the `./build/install/symly/`. The application can be run
using `./build/install/symly/bin/symly <ARGS>` or using `./build/install/symly/bin/symly.bat` on Windows.

```txt
$ ./build/install/symly/bin/symly

Usage: symly [-hv] [COMMAND]
symly create links
  -h, --help      Prints this help message and exits
  -v, --verbose   Be verbose.
Commands:
  link  link
```
