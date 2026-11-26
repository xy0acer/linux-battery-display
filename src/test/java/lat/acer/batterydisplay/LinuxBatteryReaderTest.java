package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinuxBatteryReaderTest {
    @TempDir
    Path root;

    private void device(String name, String type, String capacity) throws IOException {
        Path directory = Files.createDirectory(root.resolve(name));
        Files.writeString(directory.resolve("type"), type + "\n");
        if (capacity != null) {
            Files.writeString(directory.resolve("capacity"), capacity + "\n");
        }
    }

    private LinuxBatteryReader reader() {
        return new LinuxBatteryReader(root);
    }

    @Test
    void readsLaptopBatteryAndIgnoresCharger() throws IOException {
        device("AC0", "Mains", null);
        device("BAT0", "Battery", "37");
        assertEquals(37, reader().readState().percentage().orElseThrow());
    }

    @Test
    void acceptsEmptyAndFullBatteries() throws IOException {
        device("BAT0", "Battery", "0");
        assertEquals(0, reader().readState().percentage().orElseThrow());
        Files.writeString(root.resolve("BAT0/capacity"), "100\n");
        assertEquals(100, reader().readState().percentage().orElseThrow());
    }

    @Test
    void handlesMissingDirectoryAndNoBatteries() {
        assertTrue(new LinuxBatteryReader(root.resolve("missing")).readState().percentage().isEmpty());
        assertTrue(reader().readState().percentage().isEmpty());
    }

    @Test
    void skipsBrokenReadingsButKeepsGoodBattery() throws IOException {
        device("missing", "Battery", null);
        device("invalid", "Battery", "not a number");
        device("negative", "Battery", "-1");
        device("tooHigh", "Battery", "101");
        device("BAT0", "Battery", "64");
        assertEquals(64, reader().readState().percentage().orElseThrow());
    }

    @Test
    void reportsUnknownWhenAllReadingsAreInvalid() throws IOException {
        device("BAT0", "Battery", "unknown");
        assertTrue(reader().readState().percentage().isEmpty());
    }

    @Test
    void skipsAbsentBatteries() throws IOException {
        device("BAT0", "Battery", "80");
        Files.writeString(root.resolve("BAT0/present"), "0\n");
        assertTrue(reader().readState().percentage().isEmpty());
    }

    @Test
    void skipsPeripheralBatteries() throws IOException {
        device("mouse", "Battery", "90");
        Files.writeString(root.resolve("mouse/scope"), "Device\n");
        device("BAT0", "Battery", "30");
        Files.writeString(root.resolve("BAT0/scope"), "System\n");
        assertEquals(30, reader().readState().percentage().orElseThrow());
    }

    @Test
    void averagesMultipleBatteriesWithAnyNames() throws IOException {
        device("main-battery", "Battery", "60");
        device("secondary-battery", "Battery", "81");
        assertEquals(71, reader().readState().percentage().orElseThrow());
    }

    @Test
    void chargingDoesNotChangeHowPercentageIsRead() throws IOException {
        device("BAT0", "Battery", "42");
        Files.writeString(root.resolve("BAT0/status"), "Charging\n");
        assertEquals(42, reader().readState().percentage().orElseThrow());
    }

    @Test
    void refreshesRatherThanCachingOldBatteryValue() throws IOException {
        device("BAT0", "Battery", "37");
        var reader = reader();
        assertEquals(37, reader.readState().percentage().orElseThrow());
        Files.writeString(root.resolve("BAT0/capacity"), "36\n");
        assertEquals(36, reader.readState().percentage().orElseThrow());
        Files.delete(root.resolve("BAT0/capacity"));
        assertTrue(reader.readState().percentage().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Charging", "Discharging", "Full", "Not charging", "Unknown"})
    void onlyActiveChargingEnablesBolt(String status) throws IOException {
        device("BAT0", "Battery", "42");
        Files.writeString(root.resolve("BAT0/status"), status + "\n");
        var state = reader().readState();
        assertEquals(42, state.percentage().orElseThrow());
        assertEquals(status.equals("Charging"), state.charging());
    }

    @Test
    void missingStatusKeepsPercentageWithoutBolt() throws IOException {
        device("BAT0", "Battery", "42");
        var state = reader().readState();
        assertEquals(42, state.percentage().orElseThrow());
        assertFalse(state.charging());
    }

    @Test
    void unreadableStatusKeepsPercentageWithoutBolt() throws IOException {
        device("BAT0", "Battery", "42");
        Files.createDirectory(root.resolve("BAT0/status"));
        var state = reader().readState();
        assertEquals(42, state.percentage().orElseThrow());
        assertFalse(state.charging());
    }

    @Test
    void anyIncludedChargingBatteryEnablesBolt() throws IOException {
        device("BAT0", "Battery", "60");
        device("BAT1", "Battery", "80");
        Files.writeString(root.resolve("BAT0/status"), "Discharging\n");
        Files.writeString(root.resolve("BAT1/status"), "Charging\n");
        var state = reader().readState();
        assertEquals(70, state.percentage().orElseThrow());
        assertTrue(state.charging());
    }

    @Test
    void excludedBatteriesCannotEnableBolt() throws IOException {
        device("mouse", "Battery", "90");
        Files.writeString(root.resolve("mouse/scope"), "Device\n");
        device("absent", "Battery", "80");
        Files.writeString(root.resolve("absent/present"), "0\n");
        device("invalid", "Battery", "not a number");
        for (String name : new String[] {"mouse", "absent", "invalid"}) {
            Files.writeString(root.resolve(name).resolve("status"), "Charging\n");
        }
        device("BAT0", "Battery", "42");
        assertFalse(reader().readState().charging());
    }

    @Test
    void updatesChargingStatusOnNextReading() throws IOException {
        device("BAT0", "Battery", "42");
        Path status = root.resolve("BAT0/status");
        Files.writeString(status, "Charging\n");
        var reader = reader();
        assertTrue(reader.readState().charging());
        Files.writeString(status, "Discharging\n");
        assertFalse(reader.readState().charging());
        Files.writeString(status, "Not charging\n");
        assertFalse(reader.readState().charging());
        Files.delete(root.resolve("BAT0/capacity"));
        assertEquals(BatteryState.UNKNOWN, reader.readState());
    }
}
