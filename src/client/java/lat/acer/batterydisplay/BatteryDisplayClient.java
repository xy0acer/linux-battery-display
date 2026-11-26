package lat.acer.batterydisplay;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BatteryDisplayClient implements ClientModInitializer {
    private static final int MARGIN = 6;
    private static final int TEXTURE_SIZE = 16;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_TOP = 3;
    private static final int ICON_HEIGHT = 11;
    // Default-font digits and '%' have 7 visible rows; lineHeight includes spacing.
    private static final int TEXT_HEIGHT = 7;
    private static final int ICON_TEXT_GAP = 3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BOLT_SIZE = 6;
    private static final Identifier[] BATTERY_ICONS = createBatteryIcons();
    private static final Identifier BOLT_ICON = Identifier.fromNamespaceAndPath(
            "batterydisplay", "textures/gui/bolt.png");
    private volatile BatteryState batteryState = BatteryState.UNKNOWN;

    private static Identifier[] createBatteryIcons() {
        var icons = new Identifier[14];
        for (int index = 0; index < icons.length; index++) {
            String suffix = index < 10 ? "0" + index : Integer.toString(index);
            icons[index] = Identifier.fromNamespaceAndPath(
                    "batterydisplay", "textures/gui/battery_" + suffix + ".png");
        }
        return icons;
    }

    @Override
    public void onInitializeClient() {
        var reader = new LinuxBatteryReader(Path.of("/sys/class/power_supply"));
        var poller = Executors.newSingleThreadScheduledExecutor(task -> {
            var thread = new Thread(task, "batterydisplay-reader");
            thread.setDaemon(true);
            return thread;
        });

        // Publish percentage and charging status together so a frame sees one reading.
        poller.scheduleWithFixedDelay(() -> batteryState = reader.readState(),
                0, 5, TimeUnit.SECONDS);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> poller.shutdownNow());

        // Inherit vanilla's hide-HUD (F1) behavior from the chat layer.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("batterydisplay", "battery"),
                this::render);
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        var client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        BatteryState state = batteryState;
        String text = state.displayText();
        int groupWidth = ICON_WIDTH + ICON_TEXT_GAP + client.font.width(text);
        int x = Math.max(0, graphics.guiWidth() - groupWidth - MARGIN);
        int y = Math.max(0, graphics.guiHeight() - ICON_HEIGHT - MARGIN);

        // Crop the transparent rows: the HUD row follows the visible battery, not its canvas.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BATTERY_ICONS[state.iconIndex()],
                x, y, 0.0F, (float) ICON_TOP,
                ICON_WIDTH, ICON_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        if (state.charging()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BOLT_ICON,
                    x + ICON_WIDTH - BOLT_SIZE, y + ICON_HEIGHT - BOLT_SIZE,
                    0.0F, 0.0F, BOLT_SIZE, BOLT_SIZE, BOLT_SIZE, BOLT_SIZE);
        }
        graphics.text(client.font, text, x + ICON_WIDTH + ICON_TEXT_GAP,
                y + (ICON_HEIGHT - TEXT_HEIGHT) / 2, TEXT_COLOR, true);
    }
}
