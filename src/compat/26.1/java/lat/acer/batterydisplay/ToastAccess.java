package lat.acer.batterydisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.ToastManager;

/** Minecraft 26.1.x: the toast manager is a getter on {@code Minecraft} itself. */
final class ToastAccess {
    private ToastAccess() {
    }

    static ToastManager of(Minecraft client) {
        return client.getToastManager();
    }
}
