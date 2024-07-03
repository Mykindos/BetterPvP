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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class HealingShot extends PrepareArrowSkill implements HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    double baseDuration;

    double increaseDurationPerLevel;

    int regenerationStrength;

    private final Set<UUID> upwardsArrows = new HashSet<>();

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
                "Shoot an arrow that gives <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength) + "</effect>",
                "to allies hit for " + getValueString(this::getDuration, level) + " seconds",
                "and cleanse them of all negative effects",
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

    //Code from PrepareArrowSkill. For this skill, we need to use PreCustomDamageEvent as it effects targets we cannot damage
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

    //event to capture the initial velocity of the arrow (ensure it is going up)
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
            if (!upwardsArrows.remove(arrow.getUniqueId())) return;
            if (!arrows.contains(arrow)) return;

            Location arrowLocation = arrow.getLocation();
            for (Entity entity : arrowLocation.getWorld().getNearbyEntities(arrowLocation, 0.5, 0.5, 0.5)) {
                if (entity instanceof Player && entity.getUniqueId().equals(shooter.getUniqueId())) {
                    Location playerLocation = entity.getLocation();
                    double distanceSquared = arrowLocation.distanceSquared(playerLocation);
                    double radiusSquared = 0.4 * 0.4;
                    if (distanceSquared <= radiusSquared) {
                        int level = getLevel(shooter);
                        if (level > 0){
                            onHit(shooter, shooter,level , event);
                            return;
                        }
                    }
                }
            }
        }
    }


    public void onHit(Player damager, LivingEntity target, int level) {
        return;
    }

    public void onHit(Player damager, LivingEntity target, int level, Event event) {
        if (target instanceof Player damagee) {
            if (UtilEntity.isEntityFriendly(damager, damagee)) {

                championsManager.getEffects().addEffect(damagee, damager, EffectTypes.REGENERATION, regenerationStrength, (long) (getDuration(level) * 1000));

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
        return baseDuration + ((level - 1) * increaseDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        increaseDurationPerLevel = getConfig("increasePerLevel", 1.0, Double.class);
        regenerationStrength = getConfig("regenerationStrength", 3, Integer.class);
    }
}
