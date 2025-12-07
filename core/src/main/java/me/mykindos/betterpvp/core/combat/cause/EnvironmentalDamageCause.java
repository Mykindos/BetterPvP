package me.mykindos.betterpvp.core.combat.cause;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * Damage cause for environmental effects
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class EnvironmentalDamageCause implements DamageCause {

    private final String name;
    private final String displayName;
    private final EntityDamageEvent.DamageCause vanillaCause;
    private final boolean trueDamage;
    private final long delay;
    private final boolean knockback;
    
    /**
     * Creates an environmental damage cause with default settings
     * @param name the internal name
     * @param displayName the display name
     */
    public EnvironmentalDamageCause(String name, String displayName, EntityDamageEvent.DamageCause vanillaCause) {
        this(name, displayName, vanillaCause, false, 1000L, false);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public boolean isTrueDamage() {
        return trueDamage;
    }
    
    @Override
    public long getDefaultDelay() {
        return delay;
    }
    
    @Override
    public boolean allowsKnockback() {
        return knockback;
    }
    
    @Override
    public Collection<DamageCauseCategory> getCategories() {
        return Collections.singleton(DamageCauseCategory.ENVIRONMENTAL);
    }

    @Override
    public EntityDamageEvent.DamageCause getBukkitCause() {
        return vanillaCause;
    }
}
