package ca.anthonyting.personalplugins.commands;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledUpdateRestartTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final List<ScheduledUpdateRestart.MaintenanceWindow> WINDOWS =
            ScheduledUpdateRestart.parseWindows(List.of("03:00-04:00", "15:00-16:00"));

    @Test
    void parsesAndOrdersTwoMaintenanceWindows() {
        var windows = ScheduledUpdateRestart.parseWindows(List.of("15:00-16:00", "03:00-04:00"));

        assertEquals(LocalTime.of(3, 0), windows.get(0).start());
        assertEquals(LocalTime.of(15, 0), windows.get(1).start());
    }

    @Test
    void rejectsInvalidOrOverlappingWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> ScheduledUpdateRestart.parseWindows(List.of("03:00-04:00")));
        assertThrows(IllegalArgumentException.class,
                () -> ScheduledUpdateRestart.parseWindows(List.of("03:00-04:00", "03:30-05:00")));
        assertThrows(IllegalArgumentException.class,
                () -> ScheduledUpdateRestart.parseWindows(List.of("24:00-25:00", "15:00-16:00")));
    }

    @Test
    void selectsRandomTimeWithinTheCurrentEligibleWindow() {
        ZonedDateTime earliest = ZonedDateTime.of(2026, 11, 3, 3, 30, 0, 0, ZONE);

        ZonedDateTime selected = ScheduledUpdateRestart.findNextWindowTime(earliest, WINDOWS, ZONE);

        assertEquals(earliest.toLocalDate(), selected.toLocalDate());
        assertFalse(selected.isBefore(earliest));
        assertTrue(selected.isBefore(ZonedDateTime.of(2026, 11, 3, 4, 0, 0, 0, ZONE)));
    }

    @Test
    void selectsNextWindowOrFollowingDayWhenCurrentWindowsHavePassed() {
        ZonedDateTime nextWindow = ScheduledUpdateRestart.findNextWindowTime(
                ZonedDateTime.of(2026, 11, 3, 4, 0, 0, 0, ZONE), WINDOWS, ZONE);
        ZonedDateTime followingDay = ScheduledUpdateRestart.findNextWindowTime(
                ZonedDateTime.of(2026, 11, 3, 16, 0, 0, 0, ZONE), WINDOWS, ZONE);

        assertEquals(LocalDate.of(2026, 11, 3), nextWindow.toLocalDate());
        assertFalse(nextWindow.isBefore(ZonedDateTime.of(2026, 11, 3, 15, 0, 0, 0, ZONE)));
        assertTrue(nextWindow.isBefore(ZonedDateTime.of(2026, 11, 3, 16, 0, 0, 0, ZONE)));
        assertEquals(LocalDate.of(2026, 11, 4), followingDay.toLocalDate());
        assertTrue(followingDay.toLocalTime().isBefore(LocalTime.of(4, 0)));
    }
}
