package lat.acer.batterydisplay;

/** Pure placement math, kept outside the renderer so every corner can be tested. */
public final class HudLayout {
    public enum Corner { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT }

    public record Position(int x, int y) { }

    private HudLayout() {
    }

    public static Position place(Corner corner, int screenWidth, int screenHeight,
            int width, int height, int margin) {
        boolean right = corner == Corner.BOTTOM_RIGHT || corner == Corner.TOP_RIGHT;
        boolean bottom = corner == Corner.BOTTOM_RIGHT || corner == Corner.BOTTOM_LEFT;
        int x = right ? screenWidth - width - margin : margin;
        int y = bottom ? screenHeight - height - margin : margin;
        return new Position(Math.max(0, x), Math.max(0, y));
    }
}
