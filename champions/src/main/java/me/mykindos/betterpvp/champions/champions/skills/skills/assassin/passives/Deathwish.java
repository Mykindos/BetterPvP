package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.deathwish.DeathwishData;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.deathwish.DeathwishThreshold;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntToDoubleFunction;

@BPvPListener
@Singleton
public class Deathwish extends Skill implements Listener {

    private final Map<Player, DeathwishData> data = new WeakHashMap<>();

    private double damageIncreaseThreshold;
    private double damageIncreaseAmount;
    private double attackSpeedIncreaseThreshold;

    /**
     * Attack speed increase is a percentage, so 20 means 20% increased attack speed
     */
    private int attackSpeedIncreaseAmount;
    private double effectDuration;
    private double effectDurationIncreasePerLevel;

    @Inject
    public Deathwish(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {
        // Note: thresholds are somewhat hard coded since the damage increase threshold is always higher than the attack
        // speed increase threshold.

        final @NotNull String damageThreshold = getValueStringPercentage(this::getDamageIncreaseThreshold, level, 100);

        return new String[]{
                "At " + damageThreshold + " health, gain " + getValueString(this::getDamageIncreaseAmount, level)
                        + " increased",
                "melee damage.",
                "",
                "At " + getValueStringPercentage(this::getAttackSpeedIncreaseThreshold, level, 100) + " health, gain "
                        + getValueStringPercentage(this::getAttackSpeedIncreaseAmount, level, 1)
                        + " increased",
                "attack speed.",
                "",
                "Effects last for " + getValueString(this::getEffectDuration, level) + " seconds",
                "and end if you heal above " + damageThreshold + " health.",
        };
    }

    // <editor-fold defaultstate="collapsed" desc="Config Getters / Description Helpers">
    private @NotNull String getValueStringPercentage(@NotNull IntToDoubleFunction provider, int level, int multiplier) {
        return getValueString(provider, level, multiplier, "%", 0);
    }

    private double getDamageIncreaseThreshold(int level) {
        return damageIncreaseThreshold;
    }

    private double getDamageIncreaseAmount(int level) {
        return damageIncreaseAmount;
    }

    private double getAttackSpeedIncreaseThreshold(int level) {
        return attackSpeedIncreaseThreshold;
    }

    private double getAttackSpeedIncreaseAmount(int level) {
        return attackSpeedIncreaseAmount;
    }

    private double getEffectDuration(int level) {
        return effectDuration + (level - 1) * effectDurationIncreasePerLevel;
    }
    // </editor-fold>

    /**
     * Handles both applying the damage increase modifier when the player damages an entity and checking if the player
     * has reached a new threshold when they take damage.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) return;

        if (event.getDamagee() instanceof Player target) {
            final int level = getLevel(target);
            if (level <= 0) return;

            data.putIfAbsent(target, new DeathwishData());

            updateThreshold(target);
            return;
        }

        if (event.getDamager() instanceof Player damager && event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            final int level = getLevel(damager);
            if (level <= 0) return;

            data.putIfAbsent(damager, new DeathwishData());

            final @NotNull DeathwishData abilityData = data.get(damager);
            if (abilityData.getCurrentThreshold().getLevel() < DeathwishThreshold.DAMAGE.getLevel()) {
                // Player has not reached the damage increase threshold, so we dont apply the damage increase modifier
                return;
            }

            final double damageIncrease = getDamageIncreaseAmount(level);
            event.addModifier(new SkillDamageModifier.Flat(this, damageIncrease));

            event.addReason(getName());

            final float pitch = switch (abilityData.getCurrentThreshold()) {
                case DAMAGE -> 1f;
                case ATTACK_SPEED -> 1.5f;
                default -> 1f;  // unreachable
            };

            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_GOAT_MILK, 2f, pitch);
        }
    }

    /**
     * Checks if the player has reached a new threshold when they take damage and applies the appropriate effects.
     */
    private void updateThreshold(@NotNull Player player) {
        final DeathwishData abilityData = data.get(player);
        final double healthPercentage = UtilPlayer.getHealthPercentage(player);
        final int level = getLevel(player);
        final long currentTime = System.currentTimeMillis();

        // Since attack speed threshold is lower than damage increase threshold, we check for it first.
        if (healthPercentage <= getAttackSpeedIncreaseThreshold(level) && !abilityData.isReachedMaxThreshold()) {
            abilityData.setCurrentThreshold(DeathwishThreshold.ATTACK_SPEED);
            abilityData.setLastEffectTime(currentTime);

            final int attackSpeedIncreaseAmount = (int) getAttackSpeedIncreaseAmount(level);
            final long effectDurationMillis = (long) (getEffectDuration(level) * 1000L);

            championsManager.getEffects().addEffect(player, EffectTypes.ATTACK_SPEED, getName(), attackSpeedIncreaseAmount,
                    effectDurationMillis);

            return;
        }

        if (healthPercentage <= getDamageIncreaseThreshold(level) && !abilityData.isReachedMaxThreshold()) {
            abilityData.setCurrentThreshold(DeathwishThreshold.DAMAGE);
            abilityData.setLastEffectTime(currentTime);
            return;
        }

        // Player is above both thresholds, so we clear any active effects; player will be removed from map during
        // update event
        clearAttackSpeedEffect(player);
    }

    /**
     * This method is responsible for cleaning up the attack speed effect when the duration expires or when the player
     * heals above the damage increase threshold.
     */
    @UpdateEvent
    public void onUpdate() {
        final Iterator<Player> iterator = data.keySet().iterator();
        while (iterator.hasNext()) {
            final @Nullable Player player = iterator.next();
            final @NotNull DeathwishData abilityData = data.get(player);

            // Probably dont need is dead check and is invalid check together but can never be too safe
            if (player == null || !player.isOnline() || player.isDead() || !player.isValid()) {
                clearAttackSpeedEffect(player);
                iterator.remove();
                continue;
            }

            // Player is not null at this point so that's why the check is down here instead of at the start of the loop
            final int level = getLevel(player);
            if (level <= 0) {
                clearAttackSpeedEffect(player);
                iterator.remove();
                continue;
            }

            final long effectDurationMillis = (long) (getEffectDuration(level) * 1000L);
            final boolean hasTimeElapsed = UtilTime.elapsed(abilityData.getLastEffectTime(), effectDurationMillis);

            if (hasTimeElapsed || UtilPlayer.getHealthPercentage(player) > getDamageIncreaseThreshold(level)) {
                if (abilityData.getCurrentThreshold().getLevel() > DeathwishThreshold.NONE.getLevel()) {
                    clearAttackSpeedEffect(player);
                    iterator.remove();

                    // FX
                    spawnEndEffectParticles(player);
                    player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
                    return;
                }
            }

            spawnThresholdParticles(player, abilityData.getCurrentThreshold());
        }
    }

    private void spawnEndEffectParticles(@NotNull Player player) {
        Location base = player.getLocation();

        for (int degree = 0; degree < 360; degree += 10) {
            double radius = 0.5;
            double radians = Math.toRadians(degree);

            double addX = radius * Math.cos(radians);
            double addZ = radius * Math.sin(radians);

            Location particleLoc = base.clone().add(addX, player.getHeight() / 2, addZ);

            Particle.END_ROD
                    .builder()
                    .location(particleLoc)
                    .receivers(45)
                    .count(1)
                    .spawn();
        }
    }

    private void spawnThresholdParticles(@NotNull Player player, @NotNull DeathwishThreshold threshold) {
        Particle.DUST
                .builder()
                .color(threshold.getColor())
                .location(player.getLocation())
                .offset(0.5, 0.5, 0.5)
                .receivers(45)
                .count(2)
                .extra(0)
                .spawn();
    }

    /**
     * Removes the attack speed effect from the player if they have it. This is called when the player heals above the
     * damage increase threshold or when the effect duration expires.
     */
    private void clearAttackSpeedEffect(@Nullable Player player) {
        if (player == null) return;

        // not sure if we need a hasEffect check here
        championsManager.getEffects().removeEffect(player, EffectTypes.ATTACK_SPEED, getName());
    }

    @Override
    public String getName() {
        return "Deathwish";
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        damageIncreaseThreshold = getConfig("damageIncreaseThreshold", 0.75, Double.class);
        damageIncreaseAmount = getConfig("damageIncreaseAmount", 1.0, Double.class);
        attackSpeedIncreaseThreshold = getConfig("attackSpeedIncreaseThreshold", 0.5, Double.class);
        attackSpeedIncreaseAmount = getConfig("attackSpeedIncreaseAmount", 20, Integer.class);
        effectDuration = getConfig("effectDuration", 3.0, Double.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 2.0, Double.class);
    }
}
