package net.xolt.freecam.forge;

import net.minecraftforge.fml.ModList;
import net.xolt.freecam.platform.Platform;

public class ForgePlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
