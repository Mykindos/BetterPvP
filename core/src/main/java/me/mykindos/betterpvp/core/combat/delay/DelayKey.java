package me.mykindos.betterpvp.core.combat.delay;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Key for identifying unique damage delay combinations
 */
@Data
@RequiredArgsConstructor
public class DelayKey {
    
    /**
     * The UUID of the entity taking damage
     */
    private final UUID damageeId;
    
    /**
     * The UUID of the entity dealing damage (can be null for environmental damage)
     */
    @Nullable
    private final UUID damagerId;
    
    /**
     * The name of the damage cause
     */
    private final DamageCause cause;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DelayKey delayKey = (DelayKey) o;
        
        return Objects.equals(damageeId, delayKey.damageeId) &&
               Objects.equals(damagerId, delayKey.damagerId) &&
               Objects.equals(cause, delayKey.cause);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(damageeId, damagerId, cause);
    }
    
    @Override
    public String toString() {
        return String.format("DelayKey{damagee=%s, damager=%s, cause='%s'}", damageeId, damagerId, cause);
    }
}
