# FilePatcher
FilePatcher allows you to create and load patcher files.

It is very useful to distribute files over the internet that require another file to be used;
as such, it is very useful for EXE mods or JAR mods of which sharing the normal file
would be legally troubling.

## How to Use
FilePatcher requires Java 8 or up.

When launched without any arguments, console mode is started.
In Console Mode, there are various commands:
### `exit`
Exits the program.
### `load <zipfile>`
Loads a previously created patcher zipfile to be used on another file.
### `target <file>`
Targets a file to patch using a patcher zipfile.
### `hash`
Compares the internal hash with the hash of the target file.
The hashing function was custom made by me so I am unsure of how many collisions there are.
Requires the patcher zipfile and target file to be set.
### `apply <destination>`
Applies the patch to the target file, and saves the result in the destination parameter.
Requires the patcher zipfile and target file to be set.

However, FilePatcher.jar can also be launched with arguments:
### `--generateHash <file>`
Generates a hash for the target file, using my custom hashing program.
Result saved in `<file>_hash.bin`.
### `--generateDiff <original> <modified>`
Generates the main patcher file. The original file must be smaller than the modified file for it to work!
The result is saved in `<modified>.patcher`.
### `--compile <original> <modified> <destination>` (to-be-implemented)
Does the entire patcher creation task.
Creates `diff.patcher` and `hash.bin` files from the files provided in the arguments,
and saves the zip result to `<destination>`.

## Creating a FilePatcher zipfile
A FilePatcher zipfile has 2 entries: `hash.bin`, the hash of the original file (optional but recommended), and `diff.patcher`, which will transform the original file into the modded one.
Start by using FilePatcher.jar to create a hash of the original file using `--generateHash`, and them use it to create the patcher file using `--generateDiff`.
Rename both generated files to comply with the program's requirements and put both in a .zip file.
Now, you can share the zip with everyone! (They need to also have a copy of FilePatcher.jar of course)
