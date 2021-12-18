package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.util.DirectorySizer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TempBackup extends BukkitRunnable {

    private LinkedList<Path> directories = null;
    private final Path backupPath;
    private final Plugin main;
    private final long delay;
    private boolean havePlayersBeenOnline;
    private final BukkitTask task;

    /**
     * @param backupPath a directory to put the directories after a copy
     * @param delay the amount of time in between each backup
     */
    public TempBackup(Path backupPath, long delay, BukkitTask task) {
        this.backupPath = backupPath;
        this.delay = delay;
        this.main = Main.getPlugin();
        this.havePlayersBeenOnline = false;
        this.task = task;
    }

    /**
     * Copies paths in directories to backupDirectory while overwriting anything there.
     * @throws IOException when creating backupDirectory fails or copying to directories fails
     */
    public void runBackup() throws IOException {
        main.getLogger().info("Backing up files...");
        try {
            Files.createDirectory(backupPath);
        } catch (FileAlreadyExistsException e) {
            // then just continue
        }
        directories = getWorldDirectories();
        pack();
        main.getLogger().info("Backup success!");
    }

    // modified from https://stackoverflow.com/a/53275074/11972694
    private void pack() throws IOException {
        Path currBackup = Paths.get(backupPath.toString(), "backup.zip");

        if (Files.exists(currBackup)) {
            Files.move(currBackup, currBackup.resolveSibling("backup-prev.zip"), StandardCopyOption.REPLACE_EXISTING);
        }

        Files.createFile(currBackup);

        ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(currBackup));
        for (Path directory : directories) {
            main.getLogger().info("Backing up " + directory.getFileName() + "...");
            try (Stream<Path> paths = Files.walk(directory)) {
                String currentZipDirectory = directory.getFileName().toString();
                paths.filter(path -> !Files.isDirectory(path) && !path.getFileName().toString().equals("session.lock"))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(currentZipDirectory + File.separator + directory.relativize(path));
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        zs.close();
    }

    /**
     * Gets world directories based on server.properties (main world, nether world, and end world)
     * @return a sorted (by file size) linked list of world directories
     */
    private static LinkedList<Path> getWorldDirectories() throws IOException {

        Properties properties = new Properties();
        try (BufferedReader serverProperties = new BufferedReader(new FileReader("server.properties"))){
            properties.load(serverProperties);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String worldName = properties.getProperty("level-name");
        Path basePath = Bukkit.getWorldContainer().toPath();
        Path overworld = basePath.resolve(worldName);
        Path nether = basePath.resolve(worldName + "_nether");
        Path end = basePath.resolve(worldName + "_the_end");

        LinkedList<Path> worldDirectoriesSorted = new LinkedList<>();

        if (!overworld.toFile().exists()) {
            return worldDirectoriesSorted;
        }

        worldDirectoriesSorted.addFirst(overworld);

        DirectorySizer directorySizer = new DirectorySizer();
        Files.walkFileTree(overworld, directorySizer);
        long overworldSize = directorySizer.getDirectorySize();
        directorySizer.resetDirectorySize();

        // this section sorts worldDirectories by file size. performs well, but could be refactored for readability
        if (nether.toFile().exists()) {
            Files.walkFileTree(nether, directorySizer);
            long netherSize = directorySizer.getDirectorySize();
            directorySizer.resetDirectorySize();
            if (netherSize < overworldSize) {
                worldDirectoriesSorted.addFirst(nether);
            } else {
                worldDirectoriesSorted.addLast(nether);
            }
            if (end.toFile().exists()) {
                Files.walkFileTree(end, directorySizer);
                long endSize = directorySizer.getDirectorySize();
                directorySizer.resetDirectorySize();
                if (endSize < overworldSize && endSize < netherSize) {
                    worldDirectoriesSorted.addFirst(end);
                } else if (endSize > overworldSize && endSize > netherSize) {
                    worldDirectoriesSorted.addLast(end);
                } else {
                    worldDirectoriesSorted.add(1, end);
                }
            }
        } else if (end.toFile().exists()) { // nether does not exist but end does
            Files.walkFileTree(end, directorySizer);
            long endSize = directorySizer.getDirectorySize();
            directorySizer.resetDirectorySize();
            if (endSize < overworldSize) {
                worldDirectoriesSorted.addFirst(end);
            } else {
                worldDirectoriesSorted.addLast(end);
            }
        }
        return worldDirectoriesSorted;
    }

    @Override
    public void run() {
        try {
            if (!havePlayersBeenOnline()) {
                main.getLogger().info("No players online in " + delay + " seconds. Backup cancelled.");
                return;
            }
            synchronized (task) {
                var timeout = 10000;
                task.wait(timeout);
                runBackup();
            }
        } catch (IOException e) {
            e.printStackTrace();
            main.getLogger().warning("Error backing up files");
        } catch (InterruptedException e) {
            e.printStackTrace();
            main.getLogger().warning("Save backup task wait interrupted");
        }
    }

    public void setHavePlayersBeenOnline(boolean havePlayersBeenOnline) {
        this.havePlayersBeenOnline = havePlayersBeenOnline;
    }

    public boolean havePlayersBeenOnline() {
        if (havePlayersBeenOnline) {
            return true;
        }

        havePlayersBeenOnline = main.getServer().getOnlinePlayers().size() == 0;
        return havePlayersBeenOnline;
    }
}
