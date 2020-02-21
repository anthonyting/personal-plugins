# Personal Plugin

What this plugin does:

- adds natural player head drops from charged creepers blowing up players
- renames the console window to show the current number of players online

## How to build

The project uses Maven and the build command is `mvn clean package`.

## Notes

1) This plugin was made using Spigot API version 1.15 and Java 13. It is likely to work in earlier versions, but there is no guarantee.

2) Renaming the console windows was built for Windows and has not been tested on Linux. It may or may not work on Linux.

## Configuration

```yml
# By default, the console title will keep its original title and append the player count.
# renamed-console-title: ""

# Set this to the maximum possible length of the console title
# only if renamed-console-title is not set to anything. If renamed-console-title
# is set to something, this does nothing.
max-console-title-length: 30

disable-console-rename: false

disable-player-head-drop: false
```
