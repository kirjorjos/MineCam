package net.xolt.freecam.platform;

import java.util.ServiceLoader;

/**
 * Loader-specific information the shared module cannot obtain on its own.
 * <p>
 * Implemented once per loader and discovered through {@link ServiceLoader}, so the shared code
 * stays free of loader APIs (and of any mod it only wants to detect).
 */
public interface Platform {

    /**
     * @param modId the id to look for, e.g. {@code "minecraft"}.
     * @return whether a mod with that id is loaded.
     */
    boolean isModLoaded(String modId);

    static Platform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final Platform INSTANCE = ServiceLoader.load(Platform.class)
                .findFirst()
                .orElse(new Platform() {
                    @Override
                    public boolean isModLoaded(String modId) {
                        return false;
                    }
                });

        private Holder() {}
    }
}
