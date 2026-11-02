package net.xolt.freecam.mixins;

import net.minecraft.client.KeyMapping;
import net.xolt.freecam.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F9: the seam every synthetic dispatch passes through.
 * <p>
 * Minecheck raises its {@code InjectedInput} marker only around the vanilla call that delivers a
 * virtual key ({@code Keyboard#onKey}) or mouse button ({@code Mouse#onMouseButton}). Both end in
 * this setter, so the marker is still up here — unlike the client tick, which runs after the
 * marker is already cleared. The fork therefore records which mappings a dispatch moved, instead
 * of trying to sample the marker later.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(method = "setDown", at = @At("HEAD"))
    private void freecam$onSetDown(boolean down, CallbackInfo ci) {
        Freecam.onKeyStateChanged((KeyMapping) (Object) this, down);
    }
}
