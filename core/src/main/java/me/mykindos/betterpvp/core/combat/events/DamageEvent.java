 package me.mykindos.betterpvp.core.combat.events;

 import com.google.common.base.Preconditions;
 import com.google.common.collect.ArrayListMultimap;
 import com.google.common.collect.Multimap;
 import lombok.CustomLog;
 import lombok.Data;
 import lombok.EqualsAndHashCode;
 import lombok.Getter;
 import me.mykindos.betterpvp.core.combat.cause.DamageCause;
 import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
 import me.mykindos.betterpvp.core.combat.data.SoundProvider;
 import me.mykindos.betterpvp.core.combat.durability.DurabilityParameters;
 import me.mykindos.betterpvp.core.combat.modifiers.DamageModifier;
 import me.mykindos.betterpvp.core.combat.modifiers.DamageOperator;
 import me.mykindos.betterpvp.core.combat.modifiers.ModifierResult;
 import me.mykindos.betterpvp.core.combat.modifiers.ModifierType;
 import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
 import org.bukkit.damage.DamageSource;
 import org.bukkit.damage.DamageType;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.LightningStrike;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Projectile;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;

 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Objects;
 import java.util.Set;

/**
 * Unified damage event that handles all types of damage with proper entity separation and modifier exclusion
 * Replaces the old DamageEvent and DamageEvent classes
 */
@SuppressWarnings("UnstableApiUsage")
@EqualsAndHashCode(callSuper = true)
@CustomLog
@Data
public class DamageEvent extends CustomCancellableEvent {
    
    // Core damage information
    @NotNull
    private final Entity damagee;
    
    @Nullable
    private LivingEntity damager;        // The causing entity (player who shot arrow)
    
    @Nullable
    private final Entity directEntity;         // The direct entity (arrow itself)
    
    @NotNull
    private final DamageSource damageSource;
    
    @NotNull
    private final DamageCause cause;           // Our custom cause system
    
    @Getter
    private double damage;
    
    private final double rawDamage;
    
    // Optional entities
    @Nullable
    private LightningStrike lightning;

    // Modifiers
    private final Multimap<ModifierType, DamageModifier> modifiers = ArrayListMultimap.create();
    // Modifier exclusion system
    private final Set<Class<? extends DamageModifier>> excludedModifierTypes = new HashSet<>();
    private final Set<ModifierType> excludedModifierCategories = new HashSet<>();
    
    // Enhanced durability system
    private final DurabilityParameters durabilityParameters = new DurabilityParameters();
    
    // Combat settings
    private boolean knockback;
    private boolean hurtAnimation = true;
    private long damageDelay;
    private long forceDamageDelay = 0;
    
    // Effects and reasons
    @NotNull
    private SoundProvider soundProvider = SoundProvider.DEFAULT;
    private final Set<String> reasons = new HashSet<>();
    
    // Vanilla event handling
    private boolean doVanillaEvent = false;
    
    /**
     * Main constructor for damage events
     * @param damagee The entity taking damage
     * @param damager The causing entity (can be null for environmental damage)
     * @param directEntity The direct entity causing damage (can be null, same as damager for direct attacks)
     * @param damageSource The Bukkit damage source
     * @param cause Our custom damage cause
     * @param damage The amount of damage to deal
     */
    public DamageEvent(@NotNull Entity damagee, @Nullable LivingEntity damager, 
                         @Nullable Entity directEntity, @NotNull DamageSource damageSource,
                         @NotNull DamageCause cause, double damage) {
        super(false);
        this.damagee = damagee;
        this.damager = damager;
        this.directEntity = directEntity != null ? directEntity : damager;
        this.damageSource = damageSource;
        this.cause = cause;
        this.damage = damage;
        this.rawDamage = damage;
        this.lightning = null;
        this.damageDelay = cause.getDefaultDelay();
        this.knockback = cause.allowsKnockback();
        
        // Apply true damage settings
        if (cause.isTrueDamage()) {
            excludeModifierCategory(ModifierType.ARMOR);
        }
    }

    public DamageEvent(@NotNull Entity damagee, @Nullable LivingEntity damager,
                       @Nullable Entity directEntity, @NotNull DamageCause cause,
                       double damage) {
        this(damagee, damager, directEntity, getSource(damagee, damager), cause, damage);
    }

    public DamageEvent(@NotNull Entity damagee, @Nullable LivingEntity damager,
                       @Nullable Entity directEntity, @NotNull DamageCause cause,
                       double damage, String reason) {
        this(damagee, damager, directEntity, getSource(damagee, damager), cause, damage);
        this.reasons.add(reason);
    }
    
    /**
     * Constructor with lightning strike
     */
    public DamageEvent(@NotNull Entity damagee, @Nullable LivingEntity damager,
                         @Nullable Entity directEntity, @NotNull DamageSource damageSource,
                         @NotNull DamageCause cause, double damage, @NotNull LightningStrike lightning) {
        this(damagee, damager, directEntity, damageSource, cause, damage);
        setLightning(lightning);
    }
    
    /**
     * Constructor with reason
     */
    public DamageEvent(@NotNull Entity damagee, @Nullable LivingEntity damager,
                         @Nullable Entity directEntity, @NotNull DamageSource damageSource,
                         @NotNull DamageCause cause, double damage, @NotNull String reason) {
        this(damagee, damager, directEntity, damageSource, cause, damage);
        this.reasons.add(reason);
    }

    private static DamageSource getSource(Entity damager, Entity damagingEntity) {
        if (damagingEntity != null) {
            return DamageSource.builder(DamageType.GENERIC)
                    .withCausingEntity(damager)
                    .withDirectEntity(damagingEntity)
                    .withDamageLocation(damager.getLocation())
                    .build();
        } else if (damager != null) {
            return DamageSource.builder(DamageType.GENERIC)
                    .withDirectEntity(damager)
                    .withCausingEntity(damager)
                    .withDamageLocation(damager.getLocation())
                    .build();
        } else {
            return DamageSource.builder(DamageType.GENERIC)
                    .build();
        }
    }
    
    // Modifier exclusion methods
    
    /**
     * Excludes a specific modifier type from being applied
     * @param modifierType the modifier type to exclude
     */
    public void excludeModifierType(Class<? extends DamageModifier> modifierType) {
        excludedModifierTypes.add(modifierType);
    }
    
    /**
     * Excludes all modifiers of a specific category
     * @param category the modifier category to exclude
     */
    public void excludeModifierCategory(ModifierType category) {
        excludedModifierCategories.add(category);
    }
    
    /**
     * Checks if a modifier is excluded from this damage event
     * @param modifier the modifier to check
     * @return true if the modifier should not be applied
     */
    public boolean isModifierExcluded(DamageModifier modifier) {
        return excludedModifierTypes.contains(modifier.getClass()) ||
               excludedModifierCategories.contains(modifier.getType());
    }
    
    // Convenience methods for common exclusions
    
    /**
     * Excludes all armor-based damage reduction
     */
    public void excludeArmorReduction() {
        excludeModifierCategory(ModifierType.ARMOR);
    }
    
    /**
     * Excludes potion effect modifiers
     */
    public void excludePotionEffects() {
        excludeModifierCategory(ModifierType.EFFECT);
    }
    
    /**
     * Excludes skill-based modifiers
     */
    public void excludeSkillModifiers() {
        excludeModifierCategory(ModifierType.ABILITY);
    }
    
    // Utility methods
    
    /**
     * Gets the direct entity causing damage (for projectiles, this is the projectile itself)
     * @return the direct entity
     */
    @Nullable
    public Entity getDamagingEntity() {
        return directEntity;
    }
    
    /**
     * Gets the projectile if this damage was caused by one
     * @return the projectile or null
     */
    @Nullable
    public Projectile getProjectile() {
        return damageSource.isIndirect() && damageSource.getDirectEntity() instanceof Projectile projectile ? projectile : null;
    }

    /**
     * Checks if this damage was caused by a projectile
     * @return true if the damage was caused by a projectile
     */
    public boolean isProjectile() {
        return damageSource.isIndirect() && damageSource.getDirectEntity() instanceof Projectile;
    }
    
    /**
     * Adds a reason for this damage
     * @param reason the reason to add
     */
    public void addReason(String reason) {
        this.reasons.add(reason);
    }
    
    /**
     * Gets all reasons for this damage
     * @return array of reasons
     */
    public String[] getReasons() {
        List<String> reasons = new ArrayList<>(this.reasons);

        List<DamageModifier> ordered = getAppliedModifiers();
        for (DamageModifier modifier : ordered) {
            ModifierResult result = modifier.apply(this);
            if (!result.isReductive()) {
                reasons.add(modifier.getName());
            }
        }

        return reasons.toArray(new String[0]);
    }
    
    /**
     * Checks if this damage has a specific reason
     * @param reason the reason to check for
     * @return true if the reason exists
     */
    public boolean hasReason(String reason) {
        return this.reasons.stream().anyMatch(s -> s.equalsIgnoreCase(reason));
    }
    
    /**
     * Checks if this damage has any reasons
     * @return true if there are reasons
     */
    public boolean hasReason() {
        return !reasons.isEmpty();
    }
    
    /**
     * Sets the damage amount (ensures it's not negative)
     * @param damage the damage amount
     */
    public void setDamage(double damage) {
        this.damage = Math.max(0, damage);
    }
    
    /**
     * Checks if the damagee is a living entity
     * @return true if the damagee is living
     */
    public boolean isDamageeLiving() {
        return damagee instanceof LivingEntity;
    }
    
    /**
     * Gets the damagee as a living entity if possible
     * @return the living entity or null
     */
    @Nullable
    public LivingEntity getLivingDamagee() {
        return damagee instanceof LivingEntity living ? living : null;
    }

    /**
     * Adds a modifier to this event
     * @param modifier the modifier to add
     */
    public void addModifier(DamageModifier modifier) {
        Preconditions.checkArgument(modifier.canApply(this), "Modifier %s cannot be applied to this event", modifier.getName());
        this.modifiers.put(modifier.getType(), modifier);
    }

    /**
     * Gets all modifiers applied to this event
     * @return a multimap of modifiers by type
     */
    public Multimap<ModifierType, DamageModifier> getModifiers() {
        return ArrayListMultimap.create(modifiers);
    }

    /**
     * Gets all modifiers of a specific type applied to this event
     * @param type the modifier type to filter by
     * @return list of modifiers of the specified type
     */
    public List<DamageModifier> getModifiers(ModifierType type) {
        return List.copyOf(modifiers.get(type));
    }

    public List<DamageModifier> getAppliedModifiers() {
        List<DamageModifier> applied = new ArrayList<>();

        applied.addAll(getOrderedModifiers(false, DamageOperator.MULTIPLIER));
        applied.addAll(getOrderedModifiers(false, DamageOperator.FLAT));
        applied.addAll(getOrderedModifiers(true, DamageOperator.FLAT));
        applied.addAll(getOrderedModifiers(true, DamageOperator.MULTIPLIER));

        return applied;
    }

    private List<DamageModifier> getOrderedModifiers(boolean reductive, DamageOperator operator) {
        return modifiers.values().stream()
                .filter(Objects::nonNull)
                .filter(m -> !isModifierExcluded(m) && m.canApply(this))
                .filter(m -> {
                    ModifierResult r = m.apply(this);
                    return r.isReductive() == reductive && r.getDamageOperator() == operator;
                })
                .sorted(Comparator.comparingInt(DamageModifier::getPriority).reversed())
                .toList();
    }


    /**
     * Calculates the final modified damage after applying all modifiers in order of priority
     * @return the final modified damage
     */
    public double getModifiedDamage() {
        double damage = getDamage();

        final List<DamageModifier> additiveMultipliers = getOrderedModifiers(false, DamageOperator.MULTIPLIER);
        double base = damage;
        for (int i = 0; i < additiveMultipliers.size(); i++) {
            final ModifierResult result = additiveMultipliers.get(i).apply(this);
            if (i == 0) damage = damage * result.getDamageOperand();
            else damage = damage + (base * result.getDamageOperand());
        }

        for (DamageModifier modifier : getOrderedModifiers(false, DamageOperator.FLAT)) {
            damage = damage + modifier.apply(this).getDamageOperand();
        }

        for (DamageModifier modifier : getOrderedModifiers(true, DamageOperator.FLAT)) {
            damage = damage + modifier.apply(this).getDamageOperand();
        }

        for (DamageModifier modifier : getOrderedModifiers(true, DamageOperator.MULTIPLIER)) {
            damage = damage * modifier.apply(this).getDamageOperand();
        }

        return Math.max(0, damage);
    }


    public EntityDamageEvent.DamageCause getBukkitCause() {
        if (cause instanceof VanillaDamageCause vanillaCause) {
            return vanillaCause.getVanillaCause();
        }
        return EntityDamageEvent.DamageCause.CUSTOM;
    }

}
