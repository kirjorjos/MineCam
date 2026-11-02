package net.xolt.freecam.util;

import net.minecraft.client.player.Input;

/**
 * An {@link Input} that never reports movement.
 * <p>
 * Used to keep input from reaching whatever must not be driven: the player while the camera is
 * active, and the camera when the input came from a script (F1/F2/F9). Its type identifies it, so
 * the previous input can be restored later without clobbering another mod's input object.
 */
public class InertInput extends Input {
}
