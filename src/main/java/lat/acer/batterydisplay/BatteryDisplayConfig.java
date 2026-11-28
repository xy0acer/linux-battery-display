package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Plain JSON settings; fields missing from the file keep their defaults. */
public final class BatteryDisplayConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("batterydisplay");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_MARGIN = 50;

    public boolean enabled = true;
    public HudLayout.Corner corner = HudLayout.Corner.BOTTOM_RIGHT;
    public int margin = 2;
    public boolean showTimeRemaining = false;
    /** Hide while the charge is above this; 100 never hides. */
    public int hideAbovePercent = 100;
    public boolean hideWhileCharging = false;
    /** 0 disables the warning. */
    public int lowThreshold = 15;
    public boolean warningSound = true;
    public boolean warningToast = true;

    private transient Path file;

    /** Creates the file with defaults if missing; a broken file is reported, used as defaults, and left alone. */
    public static BatteryDisplayConfig load(Path file) {
        BatteryDisplayConfig config = null;
        if (Files.exists(file)) {
            try {
                config = GSON.fromJson(Files.readString(file), BatteryDisplayConfig.class);
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Could not read {}; using defaults without overwriting it", file, e);
                // No file reference: toggling must not replace the user's broken-but-edited file.
                return new BatteryDisplayConfig().sanitize();
            }
        }
        boolean create = config == null;
        if (create) {
            config = new BatteryDisplayConfig();
        }
        config.file = file;
        config.sanitize();
        if (create) {
            config.save();
        }
        return config;
    }

    /** No-op for a config whose file could not be parsed, protecting the user's edits. */
    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("Could not save {}", file, e);
        }
    }

    BatteryDisplayConfig sanitize() {
        if (corner == null) {
            corner = HudLayout.Corner.BOTTOM_RIGHT;
        }
        margin = clamp(margin, 0, MAX_MARGIN);
        hideAbovePercent = clamp(hideAbovePercent, 0, 100);
        lowThreshold = clamp(lowThreshold, 0, 100);
        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
