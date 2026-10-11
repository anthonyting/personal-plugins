package ca.anthonyting.personalplugins;

import ca.anthonyting.personalplugins.util.CreateSave;
import ca.anthonyting.personalplugins.util.DirectorySizer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TempBackup extends BukkitRunnable {

    private static final int MAX_BACKUP_ATTEMPTS = 5;
    private static final long RETRY_MIN_DELAY_TICKS = 6_000L;
    private static final long RETRY_MAX_DELAY_TICKS = 9_000L;

    private LinkedList<Path> directories = null;
    private final Plugin main = MainPlugin.getInstance();
    private boolean havePlayersBeenOnline;
    private final AtomicBoolean isBackupRunning = new AtomicBoolean();
    private int backupAttempts;
    private BukkitTask retryTask;

    public TempBackup() {
        this.havePlayersBeenOnline = false;
    }

    @Override
    public void run() {
        runBackupAttempt(false);
    }

    private void runBackupAttempt(boolean isRetry) {
        if (!isBackupRunning.compareAndSet(false, true)) {
            main.getLogger().info("Backup already running. Skipping backup.");
            if (isRetry) {
                scheduleRetry();
            }
            return;
        }

        if (!isRetry) {
            synchronized (this) {
                backupAttempts = 0;
                if (retryTask != null) {
                    retryTask.cancel();
                    retryTask = null;
                }
            }
        }

        try {
            this.runBackup();
            synchronized (this) {
                backupAttempts = 0;
                if (retryTask != null) {
                    retryTask.cancel();
                    retryTask = null;
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            handleBackupFailure(e);
        } finally {
            isBackupRunning.set(false);
        }
    }

    private void handleBackupFailure(Exception e) {
        int failedAttempt;
        synchronized (this) {
            failedAttempt = ++backupAttempts;
        }

        if (failedAttempt < MAX_BACKUP_ATTEMPTS) {
            long delayTicks = ThreadLocalRandom.current().nextLong(
                    RETRY_MIN_DELAY_TICKS, RETRY_MAX_DELAY_TICKS + 1);
            main.getLogger().log(Level.SEVERE,
                    "Backup attempt " + failedAttempt + " of " + MAX_BACKUP_ATTEMPTS
                            + " failed; retrying in " + (delayTicks / 20) + " seconds.",
                    e);
            scheduleRetry(delayTicks);
        } else {
            main.getLogger().log(Level.SEVERE,
                    "Backup failed after " + MAX_BACKUP_ATTEMPTS
                            + " attempts. It will be retried on the next scheduled backup.",
                    e);
        }
    }

    private void scheduleRetry() {
        scheduleRetry(ThreadLocalRandom.current().nextLong(
                RETRY_MIN_DELAY_TICKS, RETRY_MAX_DELAY_TICKS + 1));
    }

    private synchronized void scheduleRetry(long delayTicks) {
        if (retryTask == null) {
            retryTask = new BukkitRunnable() {
                @Override
                public void run() {
                    synchronized (TempBackup.this) {
                        retryTask = null;
                    }
                    runBackupAttempt(true);
                }
            }.runTaskLaterAsynchronously(main, delayTicks);
        }
    }

    /**
     * Copies paths in directories to backupDirectory while overwriting anything there.
     * @throws IOException when creating backupDirectory fails or copying to directories fails
     * @throws InterruptedException when the thread is interrupted
     */
    private void runBackup() throws IOException, InterruptedException {
        if (havePlayersBeenOffline()) {
            main.getLogger().info("No players online since last backup. Backup cancelled.");
            return;
        }

        String backupDirectoryName = main.getConfig().getString("temp-backup-directory");

        if (backupDirectoryName == null) {
            main.getLogger().warning("Backup directory not set in config. Skipping backup.");
            return;
        }

        var backupPath = Paths.get(backupDirectoryName);

        main.getLogger().info("Backing up files...");
        try {
            Files.createDirectory(backupPath);
        } catch (FileAlreadyExistsException e) {
            // then just continue
        }
        directories = getWorldDirectories();

        synchronized (this) {
            try (var ignored = new CreateSave(this)) {
                pack(backupPath);
            }
        }

        main.getLogger().info("Backup success!");
        if (main.getServer().getOnlinePlayers().isEmpty()) {
            havePlayersBeenOnline = false;
        }
    }

    // modified from https://stackoverflow.com/a/53275074/11972694
    private void pack(Path backupPath) throws IOException {
        Path currBackup = Paths.get(backupPath.toString(), "backup.zip");
        Path stagedBackup = Files.createTempFile(backupPath, "backup-", ".tmp");
        try {
            try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(
                    stagedBackup, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
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
            }

            Path previousBackup = currBackup.resolveSibling("backup-prev.zip");
            boolean currentBackupExists;
            try {
                Files.readAttributes(currBackup, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                currentBackupExists = true;
            } catch (NoSuchFileException e) {
                currentBackupExists = false;
            }

            if (currentBackupExists) {
                main.getLogger().fine("Rotating existing backup " + currBackup.toAbsolutePath()
                        + " to " + previousBackup.toAbsolutePath());
                moveAtomicallyReplacing(currBackup, previousBackup);
            }

            try {
                moveAtomicallyReplacing(stagedBackup, currBackup);
            } catch (IOException e) {
                if (currentBackupExists) {
                    try {
                        moveAtomicallyReplacing(previousBackup, currBackup);
                    } catch (IOException restoreFailure) {
                        e.addSuppressed(restoreFailure);
                    }
                }
                throw e;
            }
        } finally {
            Files.deleteIfExists(stagedBackup);
        }
    }

    private static void moveAtomicallyReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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

    public void setHavePlayersBeenOnline(boolean havePlayersBeenOnline) {
        this.havePlayersBeenOnline = havePlayersBeenOnline;
    }

    public boolean havePlayersBeenOffline() {
        return !havePlayersBeenOnline;
    }
}
