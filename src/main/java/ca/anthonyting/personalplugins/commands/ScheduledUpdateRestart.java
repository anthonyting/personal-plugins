package ca.anthonyting.personalplugins.commands;

import ca.anthonyting.personalplugins.MainPlugin;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledUpdateRestart {

    private static final String CONFIG_PATH = "server-updates.";
    private static final long MIN_INTERVAL_MILLIS = Duration.ofHours(12).toMillis();
    private static final long MAX_INTERVAL_DAYS = 3650;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final MainPlugin plugin;
    private final ServerStopTask serverStopTask;
    private final ZoneId zone = ZoneId.systemDefault();
    private ScheduledExecutorService scheduler;

    public ScheduledUpdateRestart(MainPlugin plugin, ServerStopTask serverStopTask) {
        this.plugin = plugin;
        this.serverStopTask = serverStopTask;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean(CONFIG_PATH + "enabled")) {
            return;
        }

        List<MaintenanceWindow> windows;
        try {
            windows = parseWindows(plugin.getConfig().getStringList(CONFIG_PATH + "maintenance-windows"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Scheduled server updates disabled: " + e.getMessage());
            return;
        }

        double maxIntervalDays = plugin.getConfig().getDouble(CONFIG_PATH + "max-interval-days", 7.0);
        if (!Double.isFinite(maxIntervalDays) || maxIntervalDays < 0.5 || maxIntervalDays > MAX_INTERVAL_DAYS) {
            plugin.getLogger().severe("Scheduled server updates disabled: max-interval-days must be between 0.5 and "
                    + MAX_INTERVAL_DAYS + ".");
            return;
        }

        long warningSeconds = plugin.getConfig().getLong(CONFIG_PATH + "restart-warning-seconds", 300);
        if (warningSeconds < 1 || warningSeconds > 3600) {
            plugin.getLogger().severe("Scheduled server updates disabled: restart-warning-seconds must be between 1 and 3600.");
            return;
        }

        long maxIntervalMillis = (long) (maxIntervalDays * Duration.ofDays(1).toMillis());
        long intervalMillis = RANDOM.nextLong(MIN_INTERVAL_MILLIS, maxIntervalMillis + 1);
        ZonedDateTime earliestShutdown = ZonedDateTime.now(zone)
                .plus(Duration.ofMillis(intervalMillis))
                .plusSeconds(warningSeconds);
        ZonedDateTime shutdownAt = findNextWindowTime(earliestShutdown, windows, zone);
        ZonedDateTime warningAt = shutdownAt.minusSeconds(warningSeconds);
        long delayMillis = Math.max(0, ChronoUnit.MILLIS.between(ZonedDateTime.now(zone), warningAt));

        plugin.getLogger().info("Scheduled server update shutdown for " + shutdownAt
                + " (restart warning begins at " + warningAt + ").");
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PersonalPlugins-server-update-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.schedule(() -> plugin.getServer().getScheduler().runTask(plugin,
                () -> serverStopTask.scheduleStop(warningSeconds)), delayMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    static List<MaintenanceWindow> parseWindows(List<String> configuredWindows) {
        if (configuredWindows.size() != 2) {
            throw new IllegalArgumentException("exactly two maintenance-windows must be configured.");
        }

        List<MaintenanceWindow> windows = new ArrayList<>();
        for (String configuredWindow : configuredWindows) {
            String[] times = configuredWindow.split("-", -1);
            if (times.length != 2) {
                throw new IllegalArgumentException("invalid maintenance window '" + configuredWindow
                        + "'; expected HH:mm-HH:mm.");
            }

            try {
                LocalTime start = LocalTime.parse(times[0], TIME_FORMAT);
                LocalTime end = LocalTime.parse(times[1], TIME_FORMAT);
                if (!start.isBefore(end)) {
                    throw new IllegalArgumentException("maintenance window '" + configuredWindow
                            + "' must end after it starts and may not cross midnight.");
                }
                windows.add(new MaintenanceWindow(start, end));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("invalid maintenance window '" + configuredWindow
                        + "'; expected HH:mm-HH:mm.");
            }
        }

        windows.sort(Comparator.comparing(MaintenanceWindow::start));
        if (windows.get(0).end().isAfter(windows.get(1).start())) {
            throw new IllegalArgumentException("maintenance windows must not overlap.");
        }
        return List.copyOf(windows);
    }

    static ZonedDateTime findNextWindowTime(ZonedDateTime earliest, List<MaintenanceWindow> windows, ZoneId zone) {
        for (LocalDate date = earliest.toLocalDate(); ; date = date.plusDays(1)) {
            for (MaintenanceWindow window : windows) {
                ZonedDateTime windowStart = date.atTime(window.start()).atZone(zone);
                ZonedDateTime windowEnd = date.atTime(window.end()).atZone(zone);
                ZonedDateTime eligibleStart = earliest.isAfter(windowStart) ? earliest : windowStart;
                if (eligibleStart.isBefore(windowEnd)) {
                    long windowNanos = ChronoUnit.NANOS.between(eligibleStart.toInstant(), windowEnd.toInstant());
                    return eligibleStart.plusNanos(RANDOM.nextLong(windowNanos));
                }
            }
        }
    }

    record MaintenanceWindow(LocalTime start, LocalTime end) {
    }
}
