package me.mykindos.betterpvp.core.combat.offhand;

import com.google.inject.Singleton;

/**
 * Controls the default offhand executor for clients
 */
@Singleton
public class OffhandController {

    private OffhandExecutor defaultExecutor = null;

    /**
     * Sets the default offhand executor for clients.
     * This executor will be used when a client presses the offhand key.
     *
     * @param executor the default offhand executor to set
     */
    public void setDefaultExecutor(OffhandExecutor executor) {
        this.defaultExecutor = executor;
    }

    /**
     * Gets the default offhand executor for clients.
     * This executor will be used when a client presses the offhand key.
     *
     * @return the default offhand executor
     */
    public OffhandExecutor getDefaultExecutor() {
        return defaultExecutor;
    }

}
