package me.mykindos.betterpvp.core.interaction.combat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.interaction.Interaction;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Damage cause for environmental effects
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class InteractionDamageCause implements DamageCause {

    private final String name;
    private final String displayName;
    private final Set<DamageCauseCategory> categories = new HashSet<>();
    @With
    private EntityDamageEvent.DamageCause bukkitCause = EntityDamageEvent.DamageCause.CUSTOM;
    private final boolean trueDamage;
    private final long delay;
    private final boolean knockback;

    /**
     * Creates an environmental damage cause with default settings
     * @param interaction The ability to create a damage cause for
     */
    public InteractionDamageCause(Interaction interaction) {
        this(interaction.getName(), interaction.getName(), false, DEFAULT_DELAY, false);
    }

    public InteractionDamageCause withCategory(DamageCauseCategory category) {
        categories.add(category);
        return this;
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
        return Stream.concat(categories.stream(), Stream.of(DamageCauseCategory.ABILITY)).toList();
    }

    @Override
    public EntityDamageEvent.DamageCause getBukkitCause() {
        return bukkitCause;
    }
}
