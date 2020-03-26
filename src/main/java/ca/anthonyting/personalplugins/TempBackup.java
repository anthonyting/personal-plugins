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
    private Plugin main;

    /**
     *
     * @param directories a path list of directories to copy
     * @param backupPath a directory to put the directories after a copy
     */
    public TempBackup(Path[] directories, Path backupPath) {
        this.directories = directories;
        this.backupPath = backupPath;
        this.main = Main.getPlugin();
    }

    /**
     * Copies paths in directories to backupDirectory while overwriting anything there.
     * @throws IOException when creating backupDirectory fails or copying to directories fails
     */
    public void runBackup() throws IOException {
//        main.getServer().dispatchCommand(main.getServer().getConsoleSender(), "save-all");
//        main.getLogger().info("Backing up files...");
        try {
            Files.createDirectory(backupPath);
        } catch (FileAlreadyExistsException e) {
            // then just continue
        }
        pack();
//        main.getLogger().info("Backup success!");
    }

    // modified from https://stackoverflow.com/a/53275074/11972694
    public void pack() throws IOException {
        Path p = Paths.get(backupPath.toString(), "backup.zip");

        if (Files.exists(p) && !p.toFile().delete()) {
            throw new IOException("Error deleting existing backup.zip!");
        }

        Files.createFile(p);

        ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
        for (Path directory : directories) {
//            main.getLogger().info("Backing up " + directory.getFileName() + "...");
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

        return new Path[] {overworld, nether, end};
    }

    @Override
    public void run() {
        try {
            runBackup();
        } catch (IOException e) {
            e.printStackTrace();
//            main.getLogger().warning("Error backing up files");
        }
    }
}
