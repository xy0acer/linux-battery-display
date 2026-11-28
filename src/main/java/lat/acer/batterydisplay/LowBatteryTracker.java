package lat.acer.batterydisplay;

/** Decides whether the charge is low and fires the alert once per downward threshold crossing. */
public final class LowBatteryTracker {
    /** Charge must rise this far above the threshold before the alert can fire again. */
    static final int REARM_MARGIN = 2;

    private boolean low;
    private boolean armed = true;

    public boolean isLow() {
        return low;
    }

    /** A threshold of 0 disables the warning. Returns true when the alert should be announced. */
    public boolean update(BatteryState state, int threshold) {
        if (state.percentage().isEmpty()) {
            low = false;
            return false;
        }
        if (state.charging()) {
            low = false;
            armed = true;
            return false;
        }

        int percentage = state.percentage().getAsInt();
        if (percentage >= threshold + REARM_MARGIN) {
            armed = true;
        }
        low = threshold > 0 && percentage <= threshold;
        if (low && armed) {
            armed = false;
            return true;
        }
        return false;
    }
}
