# Personal Plugin

What this plugin does:

- adds natural player head drops from charged creepers blowing up players
- renames the console window to show the current number of players online
- adds a periodic backup of world files (as a zip)
- periodic backup asynchronously saves the last two versions of the worlds in `backup.zip` and `backup-prev.zip`
- stops spawners from spawning if too many of its creations are nearby
- disable donkey dupe glitch (redundant in 1.16+
- adds a command to getstats of users with tab completion for documentation

## How to build

The project uses Maven and the build command is `mvn clean package`.

## Notes

1) This plugin is used with Paper API version 1.21.5 and Java 21. There is no guarantee that it will work in other versions.

2) Renaming the console windows was built for a Windows GUI running the server, and will not work anywhere else

## Configuration

```yml
# renamed-console-title: ""

# Set this to the maximum possible length of the console title
# only if renamed-console-title is not set to anything. If renamed-console-title
# is set to something, this does nothing.
max-console-title-length: 30

disable-console-rename: false

disable-player-head-drop: false

allow-many-spawner-mobs: false

allow-donkey-dupe: false

temp-backup-directory: ''

# frequency of backups in seconds
backup-freq: 3600
```
