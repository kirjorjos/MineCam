package net.xolt.freecam.platform;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Presence of and input dispatch by Minecheck, the harness this fork is built for.
 * <p>
 * Both answers come from outside this mod: presence from the loader, and the dispatch window from
 * Minecheck's own marker, {@code net.minecheck.platform.api.InjectedInput}. That class is reached
 * <em>reflectively</em> — never compiled against — so the fork still builds and runs standalone
 * (F10) and does not need to be built after Minecheck.
 */
public final class Minecheck {

    public static final String MOD_ID = "minecheck";

    /** Minecheck's marker: {@code public static boolean active()}. */
    private static final String INJECTED_INPUT = "net.minecheck.platform.api.InjectedInput";

    private static Boolean present = null;
    private static Method injectedInputActive = null;

    public static boolean isPresent() {
        if (present == null) {
            present = Platform.get().isModLoaded(MOD_ID);
        }
        return present;
    }

    /**
     * Equivalent to Minecheck's {@code InjectedInput.active()}.
     * <p>
     * The marker is a plain static flag that Minecheck raises around every synthetic key/mouse
     * dispatch, so this must be cheap and must never throw: a missing or renamed marker means "not
     * dispatching", which is the standalone answer (F10).
     *
     * @return whether Minecheck is dispatching virtual input right now.
     */
    public static boolean isDispatchingInput() {
        if (!isPresent()) {
            return false;
        }

        Method active = activeMethod();
        if (active == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(active.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A marker that cannot be called is a marker that is not reporting a dispatch.
            return false;
        }
    }

    /**
     * Resolves {@code InjectedInput#active}, caching it on success.
     * <p>
     * Not cached on failure: a lookup only fails while the marker is genuinely absent, and
     * re-checking keeps a late-loaded or reloaded Minecheck working without an ordering assumption.
     */
    private static @Nullable Method activeMethod() {
        Method cached = injectedInputActive;
        if (cached != null) {
            return cached;
        }

        try {
            Class<?> marker = Class.forName(INJECTED_INPUT, true, Minecheck.class.getClassLoader());
            Method active = marker.getMethod("active");
            injectedInputActive = active;
            return active;
        } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException e) {
            return null;
        }
    }

    private Minecheck() {}
}
