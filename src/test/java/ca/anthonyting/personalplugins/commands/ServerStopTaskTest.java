package ca.anthonyting.personalplugins.commands;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerStopTaskTest {

    @Test
    void roundsRemainingWallClockTimeUpToTheNextSecond() {
        assertEquals(5, ServerStopTask.remainingSeconds(TimeUnit.SECONDS.toNanos(5)));
        assertEquals(5, ServerStopTask.remainingSeconds(TimeUnit.SECONDS.toNanos(4) + 1));
        assertEquals(1, ServerStopTask.remainingSeconds(1));
        assertEquals(0, ServerStopTask.remainingSeconds(0));
    }

    @Test
    void increasesAnnouncementFrequencyAsTheStopTimeApproaches() {
        assertEquals(3600, ServerStopTask.nextAnnouncementSeconds(3601));
        assertEquals(2700, ServerStopTask.nextAnnouncementSeconds(3600));
        assertEquals(900, ServerStopTask.nextAnnouncementSeconds(901));
        assertEquals(900, ServerStopTask.nextAnnouncementSeconds(1800));
        assertEquals(600, ServerStopTask.nextAnnouncementSeconds(900));
        assertEquals(300, ServerStopTask.nextAnnouncementSeconds(600));
        assertEquals(300, ServerStopTask.nextAnnouncementSeconds(301));
        assertEquals(60, ServerStopTask.nextAnnouncementSeconds(61));
        assertEquals(30, ServerStopTask.nextAnnouncementSeconds(60));
        assertEquals(30, ServerStopTask.nextAnnouncementSeconds(31));
        assertEquals(10, ServerStopTask.nextAnnouncementSeconds(30));
        assertEquals(10, ServerStopTask.nextAnnouncementSeconds(11));
        assertEquals(9, ServerStopTask.nextAnnouncementSeconds(10));
        assertEquals(1, ServerStopTask.nextAnnouncementSeconds(2));
    }
}
