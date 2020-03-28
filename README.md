# Personal Plugin

What this plugin does:

- adds natural player head drops from charged creepers blowing up players
- renames the console window to show the current number of players online
- adds a periodic backup of world files (as a zip) with delay and backup directory specified in config.yml
- periodic backup saves the last two versions of the worlds in `backup.zip` and `backup-prev.zip`

## How to build

The project uses Maven and the build command is `mvn clean package`.

## Notes

1) This plugin was made using Spigot API version 1.15 and Java 11. There is no guarantee that it will work in other versions.

2) Renaming the console windows was built for Windows and has not been tested on Linux.

## Configuration

```yml
# renamed-console-title: ""

# Set this to the maximum possible length of the console title
# only if renamed-console-title is not set to anything. If renamed-console-title
# is set to something, this does nothing.
max-console-title-length: 30

disable-console-rename: false

disable-player-head-drop: false

temp-backup-directory: ''

# frequency of backups in seconds
backup-freq: 3600
```
