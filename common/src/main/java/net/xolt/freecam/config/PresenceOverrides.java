package net.xolt.freecam.config;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.InteractionResult;
import net.xolt.freecam.platform.Minecheck;

/**
 * Settings the fork owns while Minecheck is installed (F4/F8).
 * <p>
 * Applied on load and on save, so the config screen cannot turn them back on. Without Minecheck
 * nothing is touched (F10).
 */
public final class PresenceOverrides {

    static InteractionResult onConfigChange(ConfigHolder<ModConfig> holder, ModConfig config) {
        if (Minecheck.isPresent()) {
            apply(config);
        }
        return InteractionResult.PASS;
    }

    private static void apply(ModConfig config) {
        // F4: world interaction belongs to the scripted player, never to the camera.
        config.utility.allowInteract = false;

        // F8: nothing that changes pixels may be on while a capture could happen.
        config.visual.showHand = false;
        config.visual.fullBright = false;

        // F8: notifications would draw over the human's view.
        config.notification.notifyFreecam = false;
    }

    private PresenceOverrides() {}
}
