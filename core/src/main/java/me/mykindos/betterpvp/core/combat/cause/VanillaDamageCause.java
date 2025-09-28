package me.mykindos.betterpvp.core.combat.cause;

import lombok.*;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.util.TriState;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * Wrapper for vanilla Bukkit damage causes to integrate with our custom system
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class VanillaDamageCause implements DamageCause {
    
    private final EntityDamageEvent.DamageCause vanillaCause;
    @With
    private TriState trueDamage = TriState.NOT_SET;
    
    @Override
    public String getName() {
        return vanillaCause.name().toLowerCase();
    }
    
    @Override
    public String getDisplayName() {
        if (vanillaCause == EntityDamageEvent.DamageCause.FIRE || vanillaCause == EntityDamageEvent.DamageCause.FIRE_TICK
            || vanillaCause == EntityDamageEvent.DamageCause.CAMPFIRE) {
            return "Fire";
        }

        return UtilFormat.cleanString(vanillaCause.name());
    }
    
    @Override
    public boolean isTrueDamage() {
        if (trueDamage != TriState.NOT_SET) {
            return trueDamage == TriState.TRUE;
        }

        // These vanilla causes bypass armor reduction
        return switch (vanillaCause) {
            case FIRE_TICK, FALL, LAVA, FIRE, DROWNING, SUFFOCATION,
                 STARVATION, VOID, CONTACT, CRAMMING, HOT_FLOOR, 
                 FLY_INTO_WALL, KILL, MAGIC, WORLD_BORDER -> true;
            default -> false;
        };
    }
    
    @Override
    public long getDefaultDelay() {
        return switch (vanillaCause) {
            case ENTITY_ATTACK, CUSTOM, LAVA, SUFFOCATION, FIRE, FIRE_TICK -> DEFAULT_DELAY;
            case VOID, THORNS, WORLD_BORDER, CONTACT -> 500L;
            default -> 1000L;
        };
    }
    
    @Override
    public boolean allowsKnockback() {
        return switch (vanillaCause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, BLOCK_EXPLOSION -> true;
            default -> false;
        };
    }
    
    @Override
    public Collection<DamageCauseCategory> getCategories() {
        return Collections.singleton(switch (vanillaCause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> DamageCauseCategory.MELEE;
            case PROJECTILE -> DamageCauseCategory.RANGED;
            case MAGIC, WITHER, POISON -> DamageCauseCategory.MAGIC;
            case FALL, LAVA, FIRE, FIRE_TICK, DROWNING, SUFFOCATION,
                 STARVATION, VOID, CONTACT, CRAMMING, HOT_FLOOR, 
                 FLY_INTO_WALL, WORLD_BORDER -> DamageCauseCategory.ENVIRONMENTAL;
            default -> DamageCauseCategory.OTHER;
        });
    }

    @Override
    public EntityDamageEvent.DamageCause getBukkitCause() {
        return vanillaCause;
    }
}
