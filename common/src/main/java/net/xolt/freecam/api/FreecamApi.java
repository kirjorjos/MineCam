package net.xolt.freecam.api;

import net.xolt.freecam.Freecam;
import net.xolt.freecam.util.FreeCamera;
import net.xolt.freecam.util.FreecamPosition;
import org.jetbrains.annotations.Nullable;

/**
 * The only surface another mod is allowed to touch (spec §8).
 * <p>
 * The fork never toggles, poses or renders on anyone's behalf: everything here is read-only or a
 * marker that its caller owns.
 */
public final class FreecamApi {

    private static volatile boolean playerViewCapturePending = false;

    /**
     * @return whether the human's camera is currently active.
     */
    public static boolean isActive() {
        return Freecam.isEnabled();
    }

    /**
     * @return the camera's position and rotation, or {@code null} while freecam is inactive.
     */
    public static @Nullable FreecamPosition pose() {
        FreeCamera camera = Freecam.getFreeCamera();
        return camera == null ? null : new FreecamPosition(camera);
    }

    /**
     * Raises the injected-input marker around a dispatch of virtual input (D9).
     * <p>
     * Input dispatched inside this window is attributed to the player; the fork keeps the camera
     * still for the rest of the tick that observed it.
     *
     * @param injecting whether a virtual input dispatch is in progress.
     */
    public static void setInjectedInput(boolean injecting) {
        Freecam.setInjectedInput(injecting);
    }

    /**
     * @return whether virtual input is being dispatched right now.
     */
    public static boolean isInjectingInput() {
        return Freecam.isInjectingInput();
    }

    /**
     * Signals that a player-view capture is pending, so the fork can yield a frame (D19).
     * <p>
     * The fork does not render anything itself; it only carries the flag, so the cost of a capture
     * stays with its owner.
     */
    public static void requestPlayerViewCapture() {
        playerViewCapturePending = true;
    }

    /**
     * Reads and clears the pending player-view capture flag.
     *
     * @return whether a player-view capture was requested.
     */
    public static boolean consumePlayerViewCaptureRequest() {
        boolean pending = playerViewCapturePending;
        playerViewCapturePending = false;
        return pending;
    }

    private FreecamApi() {}
}
