package lat.acer.batterydisplay;

import java.util.Arrays;

import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Vanilla-style options list; edits apply live and are saved when the screen closes. */
public final class BatteryDisplayConfigScreen extends OptionsSubScreen {
    private static final String KEY = "batterydisplay.config.";

    private final BatteryDisplayConfig config;

    public BatteryDisplayConfigScreen(Screen parent, BatteryDisplayConfig config) {
        super(parent, Minecraft.getInstance().options, Component.translatable(KEY + "title"));
        this.config = config;
    }

    @Override
    protected void addOptions() {
        list.addHeader(Component.translatable(KEY + "display"));
        list.addSmall(
                bool("enabled", config.enabled, value -> config.enabled = value),
                bool("showTimeRemaining", config.showTimeRemaining,
                        value -> config.showTimeRemaining = value));
        list.addSmall(corner(), slider("margin", 0, BatteryDisplayConfig.MAX_MARGIN, config.margin,
                value -> Component.literal(Integer.toString(value)), value -> config.margin = value));

        list.addHeader(Component.translatable(KEY + "hiding"));
        list.addSmall(
                slider("hideAbovePercent", 0, 100, config.hideAbovePercent,
                        value -> value == 100 ? Component.translatable(KEY + "never")
                                : Component.literal(value + "%"),
                        value -> config.hideAbovePercent = value),
                bool("hideWhileCharging", config.hideWhileCharging,
                        value -> config.hideWhileCharging = value));

        list.addHeader(Component.translatable(KEY + "warnings"));
        list.addSmall(
                threshold("lowThreshold", config.lowThreshold, value -> config.lowThreshold = value));
        list.addSmall(
                bool("warningSound", config.warningSound, value -> config.warningSound = value),
                bool("warningToast", config.warningToast, value -> config.warningToast = value));
    }

    @Override
    public void removed() {
        super.removed();
        config.save();
    }

    private static OptionInstance<Boolean> bool(String name, boolean initial,
            java.util.function.Consumer<Boolean> listener) {
        // Lambdas bridge the listener type, which is Consumer on 26.1 and ValueUpdateListener later.
        return OptionInstance.createBoolean(KEY + name, tooltip(name), initial, value -> listener.accept(value));
    }

    private static OptionInstance<Integer> threshold(String name, int initial,
            java.util.function.Consumer<Integer> listener) {
        return slider(name, 0, 100, initial,
                value -> value == 0 ? CommonComponents.OPTION_OFF : Component.literal(value + "%"),
                listener);
    }

    private static OptionInstance<Integer> slider(String name, int min, int max, int initial,
            java.util.function.Function<Integer, Component> label,
            java.util.function.Consumer<Integer> listener) {
        return new OptionInstance<>(KEY + name, tooltip(name),
                (caption, value) -> Options.genericValueLabel(caption, label.apply(value)),
                new OptionInstance.IntRange(min, max), initial, value -> listener.accept(value));
    }

    private OptionInstance<HudLayout.Corner> corner() {
        var values = new OptionInstance.Enum<>(Arrays.asList(HudLayout.Corner.values()),
                Codec.STRING.xmap(HudLayout.Corner::valueOf, HudLayout.Corner::name));
        return new OptionInstance<>(KEY + "corner", tooltip("corner"),
                // The enum cycle button adds the caption itself; wrapping it again doubled "Corner:".
                (unused, value) -> Component.translatable(KEY + "corner." + value.name().toLowerCase()),
                values, config.corner, value -> config.corner = value);
    }

    private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
        return OptionInstance.cachedConstantTooltip(Component.translatable(KEY + name + ".tooltip"));
    }
}
