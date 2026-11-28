package lat.acer.batterydisplay;

import java.util.OptionalLong;

/** Smooths the jittery instantaneous rate so the time estimate does not jump every poll. */
public final class RemainingEstimator {
    static final double SMOOTHING = 0.3;

    private double smoothed = Double.NaN;
    private boolean lastCharging;

    public BatteryState smooth(BatteryState raw) {
        if (raw.minutesRemaining().isEmpty()) {
            smoothed = Double.NaN;
            return raw;
        }
        double minutes = raw.minutesRemaining().getAsLong();
        // Time-to-full and time-to-empty are unrelated; never blend across a switch.
        if (Double.isNaN(smoothed) || raw.charging() != lastCharging) {
            smoothed = minutes;
        } else {
            smoothed += SMOOTHING * (minutes - smoothed);
        }
        lastCharging = raw.charging();
        return raw.withMinutesRemaining(OptionalLong.of(Math.round(smoothed)));
    }
}
