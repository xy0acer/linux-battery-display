package lat.acer.batterydisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;

/** Minecraft 26.2 and newer: the toast manager hangs off {@code Minecraft.gui}. */
final class ToastAccess {
    private ToastAccess() {
    }

    static ToastManager of(Minecraft client) {
        return client.gui.toastManager();
    }
}
