package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Reads Linux's battery information without launching external commands. */
public final class LinuxBatteryReader {
    /** Estimates beyond this are noise from a near-zero rate, not a real runtime. */
    static final long MAX_MINUTES = 99 * 60;

    private final Path powerSupplyDirectory;

    public LinuxBatteryReader(Path powerSupplyDirectory) {
        this.powerSupplyDirectory = powerSupplyDirectory;
    }

    /** Multiple available batteries use a simple mean; any charging battery enables the bolt. */
    public BatteryState readState() {
        int total = 0;
        int count = 0;
        boolean charging = false;
        boolean discharging = false;
        // Energy (µWh/µW) and charge (µAh/µA) are summed separately so units never mix.
        var energy = new Totals();
        var charge = new Totals();

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
                        String status = readStatus(device);
                        charging |= status.equals("Charging");
                        discharging |= status.equals("Discharging");
                        if (!energy.add(device, "energy_now", "energy_full", "power_now")) {
                            charge.add(device, "charge_now", "charge_full", "current_now");
                        }
                    }
                } catch (IOException | NumberFormatException | SecurityException ignored) {
                    // A device may disappear or be unreadable; try the next one.
                }
            }
        } catch (IOException | SecurityException | java.nio.file.DirectoryIteratorException ignored) {
            return BatteryState.UNKNOWN;
        }

        if (count == 0) {
            return BatteryState.UNKNOWN;
        }
        Totals totals = energy.rate > 0 ? energy : charge;
        return new BatteryState(OptionalInt.of(Math.round((float) total / count)), charging,
                totals.minutes(charging, discharging));
    }

    private static String readStatus(Path device) {
        try {
            return Files.readString(device.resolve("status")).trim();
        } catch (IOException | SecurityException ignored) {
            // Missing status must not hide an otherwise valid percentage.
            return "";
        }
    }

    private static final class Totals {
        long now;
        long full;
        long rate;

        /** Adds one battery's readings; false if this unit family is unavailable for it. */
        boolean add(Path device, String nowFile, String fullFile, String rateFile) {
            try {
                long deviceNow = Long.parseLong(Files.readString(device.resolve(nowFile)).trim());
                long deviceFull = Long.parseLong(Files.readString(device.resolve(fullFile)).trim());
                // Some drivers report a negative rate while discharging.
                long deviceRate = Math.abs(Long.parseLong(Files.readString(device.resolve(rateFile)).trim()));
                now += deviceNow;
                full += deviceFull;
                rate += deviceRate;
                return true;
            } catch (IOException | NumberFormatException | SecurityException ignored) {
                return false;
            }
        }

        OptionalLong minutes(boolean charging, boolean discharging) {
            if (rate <= 0 || (!charging && !discharging)) {
                return OptionalLong.empty();
            }
            long remaining = charging ? full - now : now;
            if (remaining <= 0) {
                return OptionalLong.empty();
            }
            long minutes = Math.round(remaining * 60.0 / rate);
            return minutes > MAX_MINUTES ? OptionalLong.empty() : OptionalLong.of(minutes);
        }
    }
}
