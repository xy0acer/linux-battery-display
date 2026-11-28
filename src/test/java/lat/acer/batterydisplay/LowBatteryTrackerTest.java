package lat.acer.batterydisplay;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowBatteryTrackerTest {
    private final LowBatteryTracker tracker = new LowBatteryTracker();

    private boolean update(int percentage, boolean charging) {
        return tracker.update(new BatteryState(OptionalInt.of(percentage), charging), 15);
    }

    @Test
    void firesOnceWhileDraining() {
        assertFalse(update(16, false));
        assertFalse(tracker.isLow());
        assertTrue(update(15, false));
        assertTrue(tracker.isLow());
        assertFalse(update(14, false));
        assertFalse(update(3, false));
        assertTrue(tracker.isLow());
    }

    @Test
    void hysteresisPreventsRepeatedAlertsAroundThreshold() {
        assertTrue(update(15, false));
        assertFalse(update(16, false));
        assertFalse(tracker.isLow());
        assertFalse(update(15, false));
        assertFalse(update(17, false));
        assertTrue(update(15, false));
    }

    @Test
    void chargingClearsWarningAndRearms() {
        assertTrue(update(12, false));
        assertFalse(update(12, true));
        assertFalse(tracker.isLow());
        assertTrue(update(12, false));
    }

    @Test
    void unknownAndDisabledThresholdNeverWarn() {
        assertFalse(tracker.update(BatteryState.UNKNOWN, 15));
        assertFalse(tracker.isLow());
        assertFalse(tracker.update(new BatteryState(OptionalInt.of(0), false), 0));
        assertFalse(tracker.isLow());
    }
}
