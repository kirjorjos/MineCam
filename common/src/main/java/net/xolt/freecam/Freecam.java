package net.xolt.freecam;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.world.entity.Entity;
import net.xolt.freecam.config.ModBindings;
import net.xolt.freecam.config.ModConfig;
import net.xolt.freecam.platform.Minecheck;
import net.xolt.freecam.util.FreeCamera;
import net.xolt.freecam.util.FreecamPosition;
import net.xolt.freecam.util.InertInput;
import net.xolt.freecam.variant.api.BuildVariant;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class Freecam {

    public static final Minecraft MC = Minecraft.getInstance();
    public static final String MOD_ID = "freecam";

    private static final int CAMERA_ENTITY_ID = -420;

    private static boolean freecamEnabled = false;
    private static boolean playerControlEnabled = false;
    private static boolean disableNextTick = false;
    private static FreeCamera freeCamera;
    private static CameraType rememberedF5 = null;

    /** Level/player the camera was last attached to, so a world or dimension change is detectable. */
    private static ClientLevel lastLevel = null;
    private static LocalPlayer lastPlayer = null;

    /** Set by Minecheck around each dispatch of virtual input (F9), true only for that window. */
    private static volatile boolean injectingInput = false;
    /** Whether virtual input was dispatched since the last client tick. */
    private static boolean injectedInputObserved = false;
    /**
     * Key mappings a virtual dispatch left held down (F9).
     * <p>
     * Minecheck holds its marker only for the press/release call itself, so a key a script holds
     * would look physical on every tick in between and fly the camera. Remembering the mappings
     * until they are released is what makes a held virtual key belong to the player.
     */
    private static final Set<KeyMapping> injectedKeys = Collections.newSetFromMap(new IdentityHashMap<>());

    @ApiStatus.Internal
    public static void preTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        // F6: a world or dimension change swaps the level (and usually the player) under us.
        // Rebuild the camera and snap it to the player instead of disabling freecam (F5).
        boolean levelChanged = mc.level != lastLevel || mc.player != lastPlayer;
        lastLevel = mc.level;
        lastPlayer = mc.player;
        if (freecamEnabled && levelChanged) {
            attachCamera(mc);
        }

        // Disable if the previous tick asked us to,
        // or Freecam is restricted on the current server
        if ((disableNextTick || isRestrictedOnServer()) && isEnabled()) {
            toggle();
        }
        disableNextTick = false;

        updateInput(mc);

        // The injected-input window only lasts until the end of the tick that observed it.
        injectedInputObserved = false;
    }

    @ApiStatus.Internal
    public static void postTick(Minecraft mc) {
        ModBindings.forEach(Tickable::tick);
    }

    /**
     * Called when the client leaves its level.
     * <p>
     * F5: the active flag survives leaving and rejoining a world/server while the instance lives;
     * only the camera entity (which belongs to the level being unloaded) is torn down.
     */
    @ApiStatus.Internal
    public static void onLevelUnload() {
        lastLevel = null;
        lastPlayer = null;
        injectedKeys.clear();

        if (isEnabled()) {
            onDisable();
            onDisabled();
        }
    }

    @ApiStatus.AvailableSince("0.3.1")
    public static void toggle() {
        if (isRestrictedOnServer()) {
            if (ModConfig.INSTANCE.notification.notifyFreecam) {
                MC.player.displayClientMessage(new TranslatableComponent("msg.freecam.restrictedByConfig", MC.getCurrentServer().ip), true);
            }
            return;
        }

        if (freecamEnabled) {
            onDisableFreecam();
        } else {
            onEnableFreecam();
        }
        freecamEnabled = !freecamEnabled;
        if (!freecamEnabled) {
            onDisabled();
        }
    }

    @ApiStatus.AvailableSince("1.1.8")
    public static void switchControls() {
        // F2: with Minecheck installed the human never drives the player, so there is nothing to hand over.
        if (!isEnabled() || Minecheck.isPresent()) {
            return;
        }

        if (playerControlEnabled) {
            freeCamera.input = new KeyboardInput(MC.options);
        } else {
            MC.player.input = new KeyboardInput(MC.options);
            freeCamera.input = new Input();
        }
        playerControlEnabled = !playerControlEnabled;
    }

    private static void onEnableFreecam() {
        onEnable();
        freeCamera = new FreeCamera(CAMERA_ENTITY_ID);
        moveToPlayer();
        freeCamera.spawn();
        MC.setCameraEntity(freeCamera);

        if (ModConfig.INSTANCE.notification.notifyFreecam) {
            MC.player.displayClientMessage(new TranslatableComponent("msg.freecam.enable"), true);
        }
    }

    private static void onDisableFreecam() {
        onDisable();

        if (MC.player != null) {
            if (ModConfig.INSTANCE.notification.notifyFreecam) {
                MC.player.displayClientMessage(new TranslatableComponent("msg.freecam.disable"), true);
            }
        }
    }

    /**
     * Rebuilds the camera for the current level and puts it where the player is,
     * without touching the active flag.
     */
    private static void attachCamera(Minecraft mc) {
        if (freeCamera != null) {
            freeCamera.despawn();
        }

        onEnable();
        freeCamera = new FreeCamera(CAMERA_ENTITY_ID);
        moveToPlayer();
        freeCamera.spawn();
        mc.setCameraEntity(freeCamera);
    }

    private static void onEnable() {
        MC.smartCull = false;
        MC.gameRenderer.setRenderHand(ModConfig.INSTANCE.visual.showHand);

        rememberedF5 = MC.options.getCameraType();
        if (MC.gameRenderer.getMainCamera().isDetached()) {
            MC.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void onDisable() {
        injectedKeys.clear();
        MC.smartCull = true;
        MC.gameRenderer.setRenderHand(true);
        MC.setCameraEntity(MC.player);
        playerControlEnabled = false;
        if (freeCamera != null) {
            freeCamera.despawn();
            freeCamera.input = new Input();
            freeCamera = null;
        }

        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }
    }

    private static void onDisabled() {
        if (rememberedF5 != null) {
            MC.options.setCameraType(rememberedF5);
        }
    }

    /**
     * Decides who reads input this tick (F1/F2/F9).
     * <ul>
     *     <li>Virtual (injected) input belongs to the player.</li>
     *     <li>Physical input belongs to the camera; with Minecheck installed it never reaches the player.</li>
     *     <li>Without Minecheck, everything behaves as upstream (F10).</li>
     * </ul>
     */
    private static void updateInput(Minecraft mc) {
        // A key a script is still holding counts, not just the tick that saw the dispatch.
        boolean injected = injectedInputObserved || !injectedKeys.isEmpty();

        boolean playerDriven = isPlayerControlEnabled()
                || (!isEnabled() && (!Minecheck.isPresent() || injected));
        setPlayerInputInert(mc, !playerDriven);

        if (isEnabled()) {
            // F9: a scripted keypress must never fly the camera.
            setCameraInputSuppressed(Minecheck.isPresent() && injected && !isPlayerControlEnabled());
        }
    }

    private static void setPlayerInputInert(Minecraft mc, boolean inert) {
        if (inert) {
            if (mc.player.input instanceof InertInput || !(mc.player.input instanceof KeyboardInput)) {
                // Already inert, or another mod has taken over the player's input.
                return;
            }
            InertInput input = new InertInput();
            input.shiftKeyDown = mc.player.input.shiftKeyDown; // Makes player continue to sneak after freecam is enabled.
            mc.player.input = input;
        } else if (mc.player.input instanceof InertInput) {
            mc.player.input = new KeyboardInput(mc.options);
        }
    }

    private static void setCameraInputSuppressed(boolean suppressed) {
        if (freeCamera == null || isPlayerControlEnabled()) {
            return;
        }

        if (suppressed) {
            if (!(freeCamera.input instanceof InertInput)) {
                freeCamera.input = new InertInput();
            }
        } else if (freeCamera.input instanceof InertInput) {
            freeCamera.input = new KeyboardInput(MC.options);
        }
    }

    /**
     * Marks the start and end of Minecheck's dispatch of virtual input (F9).
     * <p>
     * The fork treats input observed inside this window as belonging to the player and keeps the
     * camera still for the rest of that tick.
     *
     * @param injecting whether a virtual input dispatch is in progress.
     */
    @ApiStatus.Internal
    public static void setInjectedInput(boolean injecting) {
        injectingInput = injecting;
        if (injecting) {
            injectedInputObserved = true;
        }
    }

    /**
     * @return whether a virtual input dispatch is in progress: the marker the fork owns (set through
     *         {@link net.xolt.freecam.api.FreecamApi}) or the one Minecheck raises itself (F9).
     */
    @ApiStatus.Internal
    public static boolean isInjectingInput() {
        return injectingInput || Minecheck.isDispatchingInput();
    }

    /**
     * Records a key mapping state change, so virtual keys can be told apart from physical ones (F9).
     * <p>
     * Called from {@code KeyMapping}'s setter — the seam Minecheck's synthetic dispatches pass
     * through — which covers both keyboard dispatches and mouse-bound mappings such as attack/use.
     *
     * @param mapping the mapping whose state changed.
     * @param down    the new state.
     */
    @ApiStatus.Internal
    public static void onKeyStateChanged(KeyMapping mapping, boolean down) {
        if (!down) {
            injectedKeys.remove(mapping);
            return;
        }

        if (isInjectingInput()) {
            injectedKeys.add(mapping);
            injectedInputObserved = true;
        }
    }

    /** Whether a mouse look dispatched by Minecheck is waiting for vanilla to apply it (F9). */
    private static boolean injectedMouseLook = false;
    /** Camera entity to restore after an injected mouse look has been handed to the player (F9). */
    private static Entity redirectedCameraEntity = null;

    /**
     * Latches a mouse look that arrived inside the injected-input window.
     * <p>
     * Called from vanilla's mouse movement handler: the delta is only turned into a rotation by
     * {@code MouseHandler#turnPlayer}, which runs outside the injection window.
     */
    @ApiStatus.Internal
    public static void markInjectedMouseLook() {
        if (isInjectingInput()) {
            injectedMouseLook = true;
        }
    }

    /**
     * Hands the latched mouse look to the player instead of the camera (F9), so a script can never
     * turn the camera.
     * <p>
     * Vanilla turns whichever entity is the camera entity, so the player is made the camera entity
     * for that one {@code turnPlayer} call. {@link #restoreMouseLookTarget()} puts it back before
     * anything renders.
     */
    @ApiStatus.Internal
    public static void redirectInjectedMouseLook() {
        if (!injectedMouseLook) {
            return;
        }
        injectedMouseLook = false;

        if (!freecamEnabled || playerControlEnabled || MC.player == null) {
            return;
        }

        Entity cameraEntity = MC.getCameraEntity();
        if (cameraEntity == null || cameraEntity == MC.player) {
            return;
        }

        redirectedCameraEntity = cameraEntity;
        MC.setCameraEntity(MC.player);
    }

    @ApiStatus.Internal
    public static void restoreMouseLookTarget() {
        if (redirectedCameraEntity == null) {
            return;
        }

        MC.setCameraEntity(redirectedCameraEntity);
        redirectedCameraEntity = null;
    }

    @ApiStatus.Experimental
    @ApiStatus.AvailableSince("1.2.3")
    public static void moveToEntity(@Nullable Entity entity) {
        if (freeCamera == null) {
            return;
        }
        if (entity == null) {
            moveToPlayer();
            return;
        }
        freeCamera.copyPosition(entity);
    }

    @ApiStatus.Experimental
    @ApiStatus.AvailableSince("1.2.3")
    public static void moveToPosition(@Nullable FreecamPosition position) {
        if (freeCamera == null) {
            return;
        }
        if (position == null) {
            moveToPlayer();
            return;
        }
        freeCamera.applyPosition(position);
    }

    @ApiStatus.Experimental
    @ApiStatus.AvailableSince("1.2.3")
    public static void moveToPlayer() {
        if (freeCamera == null) {
            return;
        }
        freeCamera.copyPosition(MC.player);
        freeCamera.applyPerspective(
                ModConfig.INSTANCE.visual.perspective,
                ModConfig.INSTANCE.collision.alwaysCheck || !(ModConfig.INSTANCE.collision.ignoreAll && BuildVariant.getInstance().cheatsPermitted())
        );
    }

    @ApiStatus.AvailableSince("0.4.0")
    public static FreeCamera getFreeCamera() {
        return freeCamera;
    }

    @ApiStatus.AvailableSince("1.2.3")
    public static void disableNextTick() {
        disableNextTick = true;
    }

    @ApiStatus.AvailableSince("0.2.2")
    public static boolean isEnabled() {
        return freecamEnabled;
    }

    @ApiStatus.Experimental
    @ApiStatus.AvailableSince("1.0.0")
    public static boolean isPlayerControlEnabled() {
        return playerControlEnabled;
    }

    @ApiStatus.Experimental
    @ApiStatus.AvailableSince("1.2.4")
    public static boolean isRestrictedOnServer() {
        ServerData server = MC.getCurrentServer();
        ModConfig.ServerRestriction mode = ModConfig.INSTANCE.servers.mode;
        if (mode == ModConfig.ServerRestriction.NONE || server == null || MC.hasSingleplayerServer()) {
            return false;
        }

        String ip = server.ip.trim().toLowerCase();
        return switch (mode) {
            case WHITELIST -> ModConfig.INSTANCE.servers.whitelist.stream()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .noneMatch(ip::equals);
            case BLACKLIST -> ModConfig.INSTANCE.servers.blacklist.stream()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .anyMatch(ip::equals);
            default -> throw new IllegalStateException("Unexpected mode value in Freecam.isRestrictedOnServer: " + mode);
        };
    }
}
