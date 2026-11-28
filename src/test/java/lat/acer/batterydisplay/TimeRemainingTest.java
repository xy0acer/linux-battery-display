package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRemainingTest {
    @TempDir
    Path root;

    private Path battery(String name, String status, String... files) throws IOException {
        Path directory = Files.createDirectory(root.resolve(name));
        Files.writeString(directory.resolve("type"), "Battery\n");
        Files.writeString(directory.resolve("capacity"), "50\n");
        Files.writeString(directory.resolve("status"), status + "\n");
        for (int index = 0; index < files.length; index += 2) {
            Files.writeString(directory.resolve(files[index]), files[index + 1] + "\n");
        }
        return directory;
    }

    private BatteryState read() {
        return new LinuxBatteryReader(root).readState();
    }

    @Test
    void energyDischargingUsesNowOverRate() throws IOException {
        // 30 Wh at 10 W = 3 hours.
        battery("BAT0", "Discharging",
                "energy_now", "30000000", "energy_full", "60000000", "power_now", "10000000");
        assertEquals(180, read().minutesRemaining().orElseThrow());
    }

    @Test
    void energyChargingUsesRemainingToFull() throws IOException {
        // 30 Wh missing at 20 W = 90 minutes.
        battery("BAT0", "Charging",
                "energy_now", "30000000", "energy_full", "60000000", "power_now", "20000000");
        assertEquals(90, read().minutesRemaining().orElseThrow());
    }

    @Test
    void chargeUnitsAndNegativeCurrentWork() throws IOException {
        // 2000 mAh at 1000 mA reported as negative = 2 hours.
        battery("BAT0", "Discharging",
                "charge_now", "2000000", "charge_full", "4000000", "current_now", "-1000000");
        assertEquals(120, read().minutesRemaining().orElseThrow());
    }

    @Test
    void zeroRateMissingFilesAndIdleStatusesGiveNoEstimate() throws IOException {
        battery("BAT0", "Discharging",
                "energy_now", "30000000", "energy_full", "60000000", "power_now", "0");
        assertTrue(read().minutesRemaining().isEmpty());

        Files.writeString(root.resolve("BAT0/power_now"), "10000000\n");
        for (String status : new String[] {"Full", "Not charging", "Unknown"}) {
            Files.writeString(root.resolve("BAT0/status"), status + "\n");
            assertTrue(read().minutesRemaining().isEmpty(), status);
        }

        Files.delete(root.resolve("BAT0/power_now"));
        Files.writeString(root.resolve("BAT0/status"), "Discharging\n");
        var state = read();
        assertTrue(state.minutesRemaining().isEmpty());
        assertEquals(50, state.percentage().orElseThrow());
    }

    @Test
    void absurdEstimatesFromTinyRatesAreDropped() throws IOException {
        battery("BAT0", "Discharging",
                "energy_now", "60000000", "energy_full", "60000000", "power_now", "1");
        assertTrue(read().minutesRemaining().isEmpty());
    }

    @Test
    void unitsAreNotMixedBetweenBatteries() throws IOException {
        battery("BAT0", "Discharging",
                "energy_now", "30000000", "energy_full", "60000000", "power_now", "10000000");
        battery("BAT1", "Discharging",
                "charge_now", "999999999", "charge_full", "999999999", "current_now", "1");
        assertEquals(180, read().minutesRemaining().orElseThrow());
    }

    @Test
    void multipleBatteriesShareTheCombinedRate() throws IOException {
        battery("BAT0", "Discharging",
                "energy_now", "20000000", "energy_full", "40000000", "power_now", "5000000");
        battery("BAT1", "Discharging",
                "energy_now", "10000000", "energy_full", "40000000", "power_now", "5000000");
        assertEquals(180, read().minutesRemaining().orElseThrow());
    }

    @Test
    void formatsRemainingText() {
        var state = new BatteryState(OptionalInt.of(50), false);
        assertEquals("", state.remainingText());
        assertEquals("45m", state.withMinutesRemaining(OptionalLong.of(45)).remainingText());
        assertEquals("1h00m", state.withMinutesRemaining(OptionalLong.of(60)).remainingText());
        assertEquals("2h05m", state.withMinutesRemaining(OptionalLong.of(125)).remainingText());
    }

    @Test
    void estimatorSmoothsAndResetsOnDirectionChange() {
        var estimator = new RemainingEstimator();
        var discharging = new BatteryState(OptionalInt.of(50), false);
        assertEquals(100, estimator.smooth(discharging.withMinutesRemaining(OptionalLong.of(100)))
                .minutesRemaining().orElseThrow());
        // 100 + 0.3 * (200 - 100) = 130.
        assertEquals(130, estimator.smooth(discharging.withMinutesRemaining(OptionalLong.of(200)))
                .minutesRemaining().orElseThrow());

        var charging = new BatteryState(OptionalInt.of(50), true);
        assertEquals(40, estimator.smooth(charging.withMinutesRemaining(OptionalLong.of(40)))
                .minutesRemaining().orElseThrow());

        assertTrue(estimator.smooth(charging).minutesRemaining().isEmpty());
        assertEquals(70, estimator.smooth(charging.withMinutesRemaining(OptionalLong.of(70)))
                .minutesRemaining().orElseThrow());
    }
}
