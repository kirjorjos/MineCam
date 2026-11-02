package net.xolt.freecam.mixins;

import net.minecraft.client.MouseHandler;
import net.xolt.freecam.Freecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /**
     * F9: a mouse movement that arrives while Minecheck is dispatching virtual input is latched
     * here, and kept off the camera when vanilla turns it into a rotation.
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    private void freecam$onMove(long window, double x, double y, CallbackInfo ci) {
        Freecam.markInjectedMouseLook();
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void freecam$beforeTurnPlayer(CallbackInfo ci) {
        Freecam.redirectInjectedMouseLook();
    }

    @Inject(method = "turnPlayer", at = @At("RETURN"))
    private void freecam$afterTurnPlayer(CallbackInfo ci) {
        Freecam.restoreMouseLookTarget();
    }
}
