package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

/** Reads Linux's battery information without launching external commands. */
public final class LinuxBatteryReader {
    private final Path powerSupplyDirectory;

    public LinuxBatteryReader(Path powerSupplyDirectory) {
        this.powerSupplyDirectory = powerSupplyDirectory;
    }

    /** Multiple available batteries use a simple mean; any charging battery enables the bolt. */
    public BatteryState readState() {
        int total = 0;
        int count = 0;
        boolean charging = false;

        try (var devices = Files.newDirectoryStream(powerSupplyDirectory)) {
            for (Path device : devices) {
                try {
                    if (!Files.readString(device.resolve("type")).trim().equals("Battery")) {
                        continue;
                    }
                    // Some drivers omit 'present'; when provided, respect it.
                    Path present = device.resolve("present");
                    if (Files.exists(present) && !Files.readString(present).trim().equals("1")) {
                        continue;
                    }
                    // Ignore peripheral batteries (mice/controllers), when identified.
                    Path scope = device.resolve("scope");
                    if (Files.exists(scope) && Files.readString(scope).trim().equals("Device")) {
                        continue;
                    }

                    int percentage = Integer.parseInt(Files.readString(device.resolve("capacity")).trim());
                    if (percentage >= 0 && percentage <= 100) {
                        total += percentage;
                        count++;
                        charging |= isCharging(device);
                    }
                } catch (IOException | NumberFormatException | SecurityException ignored) {
                    // A device may disappear or be unreadable; try the next one.
                }
            }
        } catch (IOException | SecurityException | java.nio.file.DirectoryIteratorException ignored) {
            return BatteryState.UNKNOWN;
        }

        return count == 0 ? BatteryState.UNKNOWN
                : new BatteryState(OptionalInt.of(Math.round((float) total / count)), charging);
    }

    private static boolean isCharging(Path device) {
        try {
            return Files.readString(device.resolve("status")).trim().equals("Charging");
        } catch (IOException | SecurityException ignored) {
            // Missing status must not hide an otherwise valid percentage.
            return false;
        }
    }
}
