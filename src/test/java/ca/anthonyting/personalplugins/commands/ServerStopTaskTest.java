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
}
