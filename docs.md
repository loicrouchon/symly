# Scenario

## Add a file to a repo

* `symly add [-s=<source-directory>] [-r] FILE... REPOSITORY`
* Examples:
  * `symly add ~/some/file somedir/repo`
  * `symly add -s /source/dir /source/dir/some/file somedir/repo`
  
Move `FILE` to `REPOSITORY` and creates a symbolic link from `FILE` its moved location.
`FILE` must exists and be located inside the source directory.
Default source directory is `~`.

The move preserve the directory of the `FILE` relative to the source directory when moving it to `REPOSITORY`.
For example: `symly add ~/some/file somedir/repo` will move `~/some/file` to `somedir/repo/some/file`.

If `FILE` is a directory, the directory will be moved and linked.
For example: `symly add ~/some/dir somedir/repo` will link the `~/some/dir` directory itself.
Whereas `symly add ~/some/dir/* somedir/repo` will link each file in the `~/some/dir`.
If some of those files are directories, they will be linked.

To only link regular files and not directory themselves, the option `-r`, `--recursive` can be passed.

If `REPO` does not exists, it will be created.

**TODO** Document how to deal with already added files

## Remove a file from a repo

* `symly rm [-s=<source-directory>] FILE REPOSITORY`
* Examples:
  * `symly rm ~/some/file somedir/repo`
  * `symly rm -s /source/dir /source/dir/some/file somedir/repo`

Remove the `TARGET` associated to the `FILE` from `REPOSITORY`.
`FILE` must exists and be located inside the source directory.
Default source directory is `~`.

If `FILE` was linked to this particular `REPOSITORY`, the `FILE` link will be deleted and the `TARGET` will be moved to `FILE`.
If `FILE` was not pointing to `TARGET`, the `TARGET` will be deleted.

**TODO** document the effect when synchronizing on a different machine after a delete: potential **orphan** link if not covered in an other repository.

## Add a file to a repository

### Option - Full path

```cmd
symly add ~/some/file ~/my/repo/some/file
```

#### Pros/cons

* **pro:** easy to understand
* **con:** error prone. For example `symly add ~/some/file ~/my/repo/file` will lead to the link `~/file -> ~/my/repo/file` on next sync even though it is clear it is not what the user initially wanted.

### Option 2 - Repo only mode

```cmd
symly add [-s=~] ~/some/file ~/my/repo
```

The path in the repository `~/my/repo` is determined by computing the relative path from `~/some/file` to the source-directory `~`.

#### Pros/cons

* **pro:** allows for consistency checks
* **con:** A bit more verbose when the source directory is not the defqult one.


# Sync

## Sync the default profile configuration

```cmd
symly link
```

## Sync a particular profile

```cmd
symly link -r linux defaults
```
