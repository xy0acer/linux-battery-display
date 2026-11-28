package lat.acer.batterydisplay;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public final class BatteryDisplayClient implements ClientModInitializer {
    private static final int TEXTURE_SIZE = 16;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_TOP = 4;
    private static final int ICON_HEIGHT = 9;
    private static final int ICON_TEXT_GAP = 3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int LOW_COLOR = 0xFFFF5555;
    private static final long BLINK_MILLIS = 500;
    private static final int BOLT_SIZE = 6;
    private static final Identifier[] BATTERY_ICONS = createBatteryIcons();
    private static final Identifier BOLT_ICON = Identifier.fromNamespaceAndPath(
            "batterydisplay", "textures/gui/bolt.png");
    private static final SystemToast.SystemToastId WARNING_TOAST = new SystemToast.SystemToastId();

    private volatile BatteryState batteryState = BatteryState.UNKNOWN;
    private final LowBatteryTracker lowBattery = new LowBatteryTracker();
    // Static so the optional Mod Menu entry point edits the same live settings.
    private static BatteryDisplayConfig config;
    private KeyMapping toggleKey;

    static BatteryDisplayConfig config() {
        return config;
    }

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
        config = BatteryDisplayConfig.load(
                FabricLoader.getInstance().getConfigDir().resolve("batterydisplay.json"));

        var reader = new LinuxBatteryReader(Path.of("/sys/class/power_supply"));
        var estimator = new RemainingEstimator();
        var poller = Executors.newSingleThreadScheduledExecutor(task -> {
            var thread = new Thread(task, "batterydisplay-reader");
            thread.setDaemon(true);
            return thread;
        });

        // Publish percentage, charging status, and estimate together so a frame sees one reading.
        poller.scheduleWithFixedDelay(() -> batteryState = estimator.smooth(reader.readState()),
                0, 5, TimeUnit.SECONDS);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> poller.shutdownNow());

        // Unbound by default so it cannot conflict; players assign it under Controls.
        var category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("batterydisplay", "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.batterydisplay.toggle",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // Inherit vanilla's hide-HUD (F1) behavior from the chat layer.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("batterydisplay", "battery"),
                this::render);
    }

    private void tick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            config.enabled = !config.enabled;
            config.save();
        }

        BatteryState state = batteryState;
        if (!lowBattery.update(state, config.lowThreshold)) {
            return;
        }
        if (config.warningSound) {
            client.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0F));
        }
        if (config.warningToast) {
            SystemToast.addOrUpdate(client.gui.toastManager(), WARNING_TOAST,
                    Component.translatable("batterydisplay.toast.low.title"),
                    Component.translatable("batterydisplay.toast.low.body", state.displayText()));
        }
    }

    private boolean hidden(BatteryState state) {
        if (!config.enabled || (config.hideWhileCharging && state.charging())) {
            return true;
        }
        return state.percentage().isPresent()
                && state.percentage().getAsInt() > config.hideAbovePercent;
    }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        var client = Minecraft.getInstance();
        BatteryState state = batteryState;
        // No usable battery (e.g. a desktop PC): draw nothing at all.
        if (client.level == null || state.percentage().isEmpty() || hidden(state)) {
            return;
        }

        String text = state.displayText();
        if (config.showTimeRemaining && state.minutesRemaining().isPresent()) {
            text += " · " + state.remainingText();
        }
        int groupWidth = ICON_WIDTH + ICON_TEXT_GAP + client.font.width(text);
        var position = HudLayout.place(config.corner, graphics.guiWidth(), graphics.guiHeight(),
                groupWidth, ICON_HEIGHT, config.margin);
        int x = position.x();
        int y = position.y();

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
                y, textColor(), true);
    }

    private int textColor() {
        if (!lowBattery.isLow()) {
            return TEXT_COLOR;
        }
        return (Util.getMillis() / BLINK_MILLIS) % 2 == 0 ? LOW_COLOR : TEXT_COLOR;
    }
}
