package me.mykindos.betterpvp.core.inventory.inventory.event;

public interface UpdateReason {
    
    /**
     * An {@link UpdateReason} that suppresses all event calls.
     */
    UpdateReason SUPPRESSED = new UpdateReason() {};
    
}
