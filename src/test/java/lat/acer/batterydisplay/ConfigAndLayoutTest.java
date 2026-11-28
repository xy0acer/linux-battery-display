package lat.acer.batterydisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigAndLayoutTest {
    @TempDir
    Path root;

    @Test
    void missingFileIsCreatedWithDefaults() throws IOException {
        Path file = root.resolve("config/batterydisplay.json");
        var config = BatteryDisplayConfig.load(file);
        assertTrue(config.enabled);
        assertEquals(HudLayout.Corner.BOTTOM_RIGHT, config.corner);
        assertEquals(2, config.margin);
        assertEquals(15, config.lowThreshold);
        assertTrue(Files.readString(file).contains("\"lowThreshold\": 15"));
    }

    @Test
    void partialFileKeepsDefaultsAndClampsValues() throws IOException {
        Path file = root.resolve("batterydisplay.json");
        Files.writeString(file, """
                {"corner": "TOP_LEFT", "margin": -4, "lowThreshold": 250, "warningSound": false}
                """);
        var config = BatteryDisplayConfig.load(file);
        assertEquals(HudLayout.Corner.TOP_LEFT, config.corner);
        assertEquals(0, config.margin);
        assertEquals(100, config.lowThreshold);
        assertFalse(config.warningSound);
        assertTrue(config.warningToast);
    }

    @Test
    void unknownCornerFallsBackToDefault() throws IOException {
        Path file = root.resolve("batterydisplay.json");
        Files.writeString(file, "{\"corner\": \"MIDDLE\"}");
        assertEquals(HudLayout.Corner.BOTTOM_RIGHT, BatteryDisplayConfig.load(file).corner);
    }

    @Test
    void malformedFileIsNeverOverwritten() throws IOException {
        Path file = root.resolve("batterydisplay.json");
        String broken = "{\"enabled\": false,, oops";
        Files.writeString(file, broken);
        var config = BatteryDisplayConfig.load(file);
        assertTrue(config.enabled);
        config.enabled = false;
        config.save();
        assertEquals(broken, Files.readString(file));
    }

    @Test
    void savePersistsToggle() throws IOException {
        Path file = root.resolve("batterydisplay.json");
        var config = BatteryDisplayConfig.load(file);
        config.enabled = false;
        config.save();
        assertFalse(BatteryDisplayConfig.load(file).enabled);
    }

    @Test
    void placesGroupInEachCorner() {
        assertEquals(new HudLayout.Position(270, 185),
                HudLayout.place(HudLayout.Corner.BOTTOM_RIGHT, 320, 200, 44, 9, 6));
        assertEquals(new HudLayout.Position(6, 185),
                HudLayout.place(HudLayout.Corner.BOTTOM_LEFT, 320, 200, 44, 9, 6));
        assertEquals(new HudLayout.Position(270, 6),
                HudLayout.place(HudLayout.Corner.TOP_RIGHT, 320, 200, 44, 9, 6));
        assertEquals(new HudLayout.Position(6, 6),
                HudLayout.place(HudLayout.Corner.TOP_LEFT, 320, 200, 44, 9, 6));
    }

    @Test
    void neverPlacesOffScreen() {
        assertEquals(new HudLayout.Position(0, 0),
                HudLayout.place(HudLayout.Corner.BOTTOM_RIGHT, 20, 5, 44, 9, 6));
    }
}
