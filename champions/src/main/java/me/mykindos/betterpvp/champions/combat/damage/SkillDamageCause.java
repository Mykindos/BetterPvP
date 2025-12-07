package me.mykindos.betterpvp.champions.combat.damage;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.cause.DamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Damage cause for skill-based damage (champion skills, clan abilities, etc.)
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class SkillDamageCause implements DamageCause {
    
    private final Skill skill;
    @With
    private final boolean trueDamage;
    private final Set<DamageCauseCategory> categories = new HashSet<>();
    private EntityDamageEvent.DamageCause bukkitCause = EntityDamageEvent.DamageCause.ENTITY_ATTACK;
    @With
    private final long delay;
    @With
    private final boolean knockback;
    
    /**
     * Creates a skill damage cause with default settings
     * @param skill the skill causing the damage
     * @param trueDamage whether this skill does true damage
     */
    public SkillDamageCause(Skill skill, boolean trueDamage) {
        this(skill, trueDamage, DEFAULT_DELAY, false);
    }
    
    /**
     * Creates a skill damage cause with no delay and knockback enabled
     * @param skill the skill causing the damage
     */
    public SkillDamageCause(Skill skill) {
        this(skill, false, DEFAULT_DELAY, false);
    }

    public SkillDamageCause withBukkitCause(EntityDamageEvent.DamageCause bukkitCause) {
        this.bukkitCause = bukkitCause;
        if (bukkitCause == EntityDamageEvent.DamageCause.PROJECTILE) {
            categories.add(DamageCauseCategory.RANGED);
        }
        return this;
    }

    public SkillDamageCause withCategory(DamageCauseCategory category) {
        categories.add(category);
        return this;
    }
    
    @Override
    public String getName() {
        return skill.getName().toLowerCase().replace(" ", "_");
    }
    
    @Override
    public String getDisplayName() {
        return skill.getName();
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
