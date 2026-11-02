package net.xolt.freecam.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.xolt.freecam.platform.Platform;

public class FabricPlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
