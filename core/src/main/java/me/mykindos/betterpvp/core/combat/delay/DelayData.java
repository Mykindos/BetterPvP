package me.mykindos.betterpvp.core.combat.delay;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Data for a damage delay entry
 */
@Data
@RequiredArgsConstructor
public class DelayData {
    
    /**
     * The timestamp when the delay was created
     */
    private final long timestamp;
    
    /**
     * The duration of the delay in milliseconds
     */
    private final long duration;
    
    /**
     * Checks if this delay has expired
     * @return true if the delay has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp >= duration;
    }
    
    /**
     * Gets the remaining time for this delay
     * @return the remaining time in milliseconds, or 0 if expired
     */
    public long getRemainingTime() {
        long elapsed = System.currentTimeMillis() - timestamp;
        return Math.max(0, duration - elapsed);
    }
}
