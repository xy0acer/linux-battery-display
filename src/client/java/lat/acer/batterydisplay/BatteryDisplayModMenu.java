package lat.acer.batterydisplay;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Only loaded when Mod Menu is installed; the mod works the same without it. */
public final class BatteryDisplayModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new BatteryDisplayConfigScreen(parent, BatteryDisplayClient.config());
    }
}
// vienvieffjv