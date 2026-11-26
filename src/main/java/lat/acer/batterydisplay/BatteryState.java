package lat.acer.batterydisplay;

import java.util.OptionalInt;

/** One immutable reading, shared between the background reader and the HUD. */
public record BatteryState(OptionalInt percentage, boolean charging) {
    public static final BatteryState UNKNOWN = new BatteryState(OptionalInt.empty(), false);
    private static final int[] ICON_UPPER_BOUNDS = {
            1, 9, 19, 27, 35, 43, 51, 59, 67, 75, 83, 91, 99, 100
    };

    public String displayText() {
        return percentage.isPresent() ? percentage.getAsInt() + "%" : "--%";
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
