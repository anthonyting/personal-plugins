package ca.anthonyting.personalplugins;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TempBackup extends BukkitRunnable {

    private Path[] directories;
    private Path backupPath;
    private final Plugin main;
    private long delay;
    private boolean havePlayersBeenOnline;
    private boolean isPlayerCountZero;

    /**
     * @param directories a path list of directories to copy
     * @param backupPath a directory to put the directories after a copy
     */
    public TempBackup(Path[] directories, Path backupPath, long delay) {
        this.directories = directories;
        this.backupPath = backupPath;
        this.delay = delay;
        this.main = Main.getPlugin();
        this.havePlayersBeenOnline = false;
        this.isPlayerCountZero = true;
    }

    /**
     * Copies paths in directories to backupDirectory while overwriting anything there.
     * @throws IOException when creating backupDirectory fails or copying to directories fails
     */
    public void runBackup() throws IOException {
        if (!havePlayersBeenOnline) {
            main.getLogger().info("No players online " + delay + " seconds. Backup cancelled.");
            return;
        } else if (isPlayerCountZero) {
            havePlayersBeenOnline = false;
        }
        main.getLogger().info("Backing up files...");
        try {
            Files.createDirectory(backupPath);
        } catch (FileAlreadyExistsException e) {
            // then just continue
        }
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
                paths.filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(currentZipDirectory + File.separator + directory.relativize(path).toString());
                    try {
                        zs.putNextEntry(zipEntry);
                        Files.copy(path, zs);
                        zs.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        zs.close();
    }

    public static Path[] getWorldDirectories() {
        Properties properties = new Properties();
        try (BufferedReader serverProperties = new BufferedReader(new FileReader("server.properties"))){
            properties.load(serverProperties);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        String worldName = properties.getProperty("level-name");
        Path basePath = Paths.get("").toAbsolutePath();
        Path overworld = basePath.resolve(worldName);
        Path nether = basePath.resolve(worldName + "_nether");
        Path end = basePath.resolve(worldName + "_the_end");

        if (nether.toFile().exists()) {
            if (end.toFile().exists()) {
                return new Path[] {overworld, nether, end};
            }
            return new Path[] {overworld, nether};
        }
        return new Path[] {overworld};
    }

    @Override
    public void run() {
        try {
            runBackup();
        } catch (IOException e) {
            e.printStackTrace();
            main.getLogger().warning("Error backing up files");
        }
    }

    public void setHavePlayersBeenOnline(boolean havePlayersBeenOnline) {
        this.havePlayersBeenOnline = havePlayersBeenOnline;
    }

    public void setPlayerCountZero(boolean playerCountZero) {
        this.isPlayerCountZero = playerCountZero;
    }

    public boolean havePlayersBeenOnline() {
        return havePlayersBeenOnline;
    }

    public boolean isPlayerCountZero() {
        return isPlayerCountZero;
    }
}
