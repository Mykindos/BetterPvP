package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class HealingShot extends PrepareArrowSkill implements HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int baseRegenerationStrength;
    private int baseDirectHitRegenerationStrength;
    private double baseNaturalRegenerationDisabledDuration;
    private double baseRadius;
    private int increaseRegenerationStrengthPerLevel;
    private int increaseDirectHitRegenerationStrengthPerLevel;
    private double increaseNaturalRegenerationDisabledDurationPerLevel;
    private double increaseRadiusPerLevel;

    private final Set<UUID> upwardsArrows = new HashSet<>();
    private final Map<UUID, Long> nonFriendlyHitTimestamps = new HashMap<>();

    @Inject
    public HealingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Healing Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives allies within " + getValueString(this::getRadius, level),
                "blocks <effect>Regeneration " + UtilFormat.getRomanNumeral(getRegenerationStrength(level)) + "</effect> for " + getValueString(this::getDuration, level) + " seconds and",
                "cleanses them of all negative effects",
                "",
                "Direct hits will give <effect>Regeneration " + UtilFormat.getRomanNumeral(getDirectHitRegenerationStrength(level)),
                "",
                "Hitting an enemy with healing shot will stop",
                "their natural regeneration for " + getValueString(this::getNaturalRegenerationDisabledDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        upwardsArrows.remove(arrow.getUniqueId());
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;
        int level = getLevel(damager);
        if (level > 0) {
            onHit(damager, cde.getDamagee(), level, event);
            arrows.remove(arrow);
            cde.addReason(getName());
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            Vector initialVelocity = arrow.getVelocity();
            int level = getLevel(shooter);
            if (level > 0 && initialVelocity.getY() > 0) {
                upwardsArrows.add(arrow.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            if (!arrows.contains(arrow)) return;

            Location arrowLocation = arrow.getLocation();
            int level = getLevel(shooter);
            if (level > 0) {
                applyAoeEffect(shooter, arrowLocation, level);
            }
        }
    }

    public void applyAoeEffect(Player shooter, Location arrowLocation, int level) {
        double radius = getRadius(level);
        List<Player> nearbyAllies = UtilPlayer.getNearbyAllies(shooter, arrowLocation, radius);
        for (Player ally : nearbyAllies) {
            championsManager.getEffects().addEffect(ally, shooter, EffectTypes.REGENERATION, getRegenerationStrength(level), (long) (getDuration(level) * 1000));
        }

        if (arrowLocation.distance(shooter.getLocation()) <= radius) {
            championsManager.getEffects().addEffect(shooter, shooter, EffectTypes.REGENERATION, getRegenerationStrength(level), (long) (getDuration(level) * 1000));
        }

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1);
        arrowLocation.getWorld().spawnParticle(Particle.REDSTONE, arrowLocation, 25, 1, 1, 1, dustOptions);
    }

    public void onHit(Player damager, LivingEntity target, int level) {
        return;
    }

    public void onHit(Player damager, LivingEntity target, int level, Event event) {
        if (target instanceof Player damagee) {
            if (UtilEntity.isEntityFriendly(damager, damagee)) {
                championsManager.getEffects().addEffect(damagee, damager, EffectTypes.REGENERATION, getDirectHitRegenerationStrength(level), (long) (getDuration(level) * 1000));

                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5F);

                championsManager.getEffects().addEffect(damagee, EffectTypes.IMMUNE, 1);
                UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>", damagee.getName(), getName(), level));
                if (!damager.equals(damagee)) {
                    UtilMessage.message(damagee, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
                }
                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
            } else {
                nonFriendlyHitTimestamps.put(damagee.getUniqueId(), (long)(System.currentTimeMillis() + (getNaturalRegenerationDisabledDuration(level) * 1000)));
                UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <alt2>%s</alt2> with <green>%s %s</green> disabling their natural regeneration.", damagee.getName(), getName(), level));
            }
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            long currentTime = System.currentTimeMillis();
            UUID playerId = player.getUniqueId();
            if (nonFriendlyHitTimestamps.containsKey(playerId) && nonFriendlyHitTimestamps.get(playerId) > currentTime) {
                if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                    event.setCancelled(true);
                }
            } else {
                nonFriendlyHitTimestamps.remove(playerId);
            }
        }
    }

    @Override
    public void displayTrail(Location location) {
        Particle.HEART.builder().location(location).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public int getRegenerationStrength(int level) {
        return baseRegenerationStrength + ((level - 1) * increaseRegenerationStrengthPerLevel);
    }

    public int getDirectHitRegenerationStrength(int level) {
        return baseDirectHitRegenerationStrength + ((level - 1) * increaseDirectHitRegenerationStrengthPerLevel);
    }

    public double getNaturalRegenerationDisabledDuration(int level) {
        return baseNaturalRegenerationDisabledDuration + ((level - 1) * increaseNaturalRegenerationDisabledDurationPerLevel);
    }

    public double getRadius(int level) {
        return baseRadius + ((level - 1) * increaseRadiusPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseRegenerationStrength = getConfig("baseRegenerationStrength", 1, Integer.class);
        increaseRegenerationStrengthPerLevel = getConfig("increaseRegenerationStrengthPerLevel", 0, Integer.class);
        baseDirectHitRegenerationStrength = getConfig("baseDirectHitRegenerationStrength", 3, Integer.class);
        increaseDirectHitRegenerationStrengthPerLevel = getConfig("increaseDirectHitRegenerationStrengthPerLevel", 0, Integer.class);
        baseNaturalRegenerationDisabledDuration = getConfig("baseNaturalRegenerationDisabledDuration", 5.0, Double.class);
        increaseNaturalRegenerationDisabledDurationPerLevel = getConfig("increaseNaturalRegenerationDisabledDurationPerLevel", 1.0, Double.class);
        baseRadius = getConfig("baseRadius", 2.0, Double.class);
        increaseRadiusPerLevel = getConfig("increaseRadiusPerLevel", 0.0, Double.class);
    }

}
