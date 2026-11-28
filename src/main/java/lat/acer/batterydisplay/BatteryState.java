package lat.acer.batterydisplay;

import java.util.OptionalInt;
import java.util.OptionalLong;

/** One immutable reading, shared between the background reader and the HUD. */
public record BatteryState(OptionalInt percentage, boolean charging, OptionalLong minutesRemaining) {
    public static final BatteryState UNKNOWN = new BatteryState(OptionalInt.empty(), false);
    private static final int[] ICON_UPPER_BOUNDS = {
            1, 9, 19, 27, 35, 43, 51, 59, 67, 75, 83, 91, 99, 100
    };

    public BatteryState(OptionalInt percentage, boolean charging) {
        this(percentage, charging, OptionalLong.empty());
    }

    public BatteryState withMinutesRemaining(OptionalLong minutes) {
        return new BatteryState(percentage, charging, minutes);
    }

    public String displayText() {
        return percentage.isPresent() ? percentage.getAsInt() + "%" : "--%";
    }

    /** Time to empty (or to full while charging), e.g. "2h05m" or "45m"; empty when unknown. */
    public String remainingText() {
        if (minutesRemaining.isEmpty()) {
            return "";
        }
        long minutes = minutesRemaining.getAsLong();
        return minutes >= 60
                ? (minutes / 60) + "h" + String.format("%02d", minutes % 60) + "m"
                : minutes + "m";
    }

    /** Unknown readings use the empty outline, with --% distinguishing them from 0%. */
    public int iconIndex() {
        int value = percentage.orElse(0);
        for (int index = 0; index < ICON_UPPER_BOUNDS.length; index++) {
            if (value <= ICON_UPPER_BOUNDS[index]) {
                return index;
            }
        }
        return ICON_UPPER_BOUNDS.length - 1;
    }
}
