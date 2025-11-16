package me.mykindos.betterpvp.core.combat.cause;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;

/**
 * Represents a cause of damage that can be extended for custom damage sources
 */
public interface DamageCause {

    long DEFAULT_DELAY = 400L;
    
    /**
     * Gets the unique name identifier for this damage cause
     * @return the name of the damage cause
     */
    String getName();
    
    /**
     * Gets the display name for this damage cause (used in messages, logs, etc.)
     * @return the display name
     */
    String getDisplayName();
    
    /**
     * Whether this damage cause bypasses all damage reduction modifiers
     * @return true if this is true damage
     */
    boolean isTrueDamage();
    
    /**
     * Gets the default damage delay for this cause in milliseconds
     * @return the default delay
     */
    long getDefaultDelay();
    
    /**
     * Whether this damage cause can apply knockback
     * @return true if knockback is allowed
     */
    boolean allowsKnockback();
    
    /**
     * Gets the categories this damage cause belongs to
     * @return the categories this cause belongs to
     */
    Collection<DamageCauseCategory> getCategories();

    /**
     * Get bukkit cause
     */
    EntityDamageEvent.DamageCause getBukkitCause();

}
