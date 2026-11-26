package lat.acer.batterydisplay;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BatteryStateTest {
    @ParameterizedTest
    @CsvSource({
            "0,0", "1,0", "2,1", "9,1", "10,2", "19,2",
            "20,3", "27,3", "28,4", "35,4", "36,5", "43,5",
            "44,6", "51,6", "52,7", "59,7", "60,8", "67,8",
            "68,9", "75,9", "76,10", "83,10", "84,11", "91,11",
            "92,12", "99,12", "100,13"
    })
    void selectsIconAtEveryRangeBoundary(int percentage, int expectedIcon) {
        assertEquals(expectedIcon, new BatteryState(OptionalInt.of(percentage), false).iconIndex());
    }

    @Test
    void unknownUsesEmptyIconAndUnknownTextWithoutBolt() {
        assertEquals(0, BatteryState.UNKNOWN.iconIndex());
        assertEquals("--%", BatteryState.UNKNOWN.displayText());
        assertFalse(BatteryState.UNKNOWN.charging());
    }

    @Test
    void chargingDoesNotChangeSelectedChargeLevel() {
        for (int percentage = 0; percentage <= 100; percentage++) {
            var discharging = new BatteryState(OptionalInt.of(percentage), false);
            var charging = new BatteryState(OptionalInt.of(percentage), true);
            assertEquals(discharging.iconIndex(), charging.iconIndex());
            assertEquals(percentage + "%", charging.displayText());
        }
    }
}
