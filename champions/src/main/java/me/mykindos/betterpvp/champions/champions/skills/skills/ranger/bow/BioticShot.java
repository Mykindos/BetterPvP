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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.*;

@Singleton
@BPvPListener
public class BioticShot extends PrepareArrowSkill implements HealthSkill, TeamSkill, BuffSkill, DefensiveSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int baseRegenerationStrength;
    private double baseNaturalRegenerationDisabledDuration;
    private int increaseRegenerationStrengthPerLevel;
    private double increaseNaturalRegenerationDisabledDurationPerLevel;
    private final Set<UUID> upwardsArrows = new HashSet<>();

    @Inject
    public BioticShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Biotic Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow that gives allies <effect>Regeneration " + UtilFormat.getRomanNumeral(getRegenerationStrength(level)) + "</effect> for",
                getValueString(this::getDuration, level) + " seconds and cleanses them of all negative effects",
                "",
                "Hitting an enemy with healing shot will",
                "give them <effect>Anti Heal</effect> for " + getValueString(this::getNaturalRegenerationDisabledDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.ANTI_HEAL.getDescription(0)

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
            arrow.remove();
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

    public void onHit(Player damager, LivingEntity target, int level) {
        return;
    }

    public void onHit(Player damager, LivingEntity target, int level, Event event) {
        if (target instanceof LivingEntity damagee) {
            if (UtilEntity.isEntityFriendly(damager, damagee)) {
                championsManager.getEffects().addEffect(damagee, damager, EffectTypes.REGENERATION, getRegenerationStrength(level), (long) (getDuration(level) * 1000));

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
                championsManager.getEffects().addEffect(damagee, damager, EffectTypes.ANTI_HEAL, 1, (long) (getNaturalRegenerationDisabledDuration(level) * 1000));
                UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <alt2>%s</alt2> with <green>%s %s</green>.", damagee.getName(), getName(), level));
                UtilMessage.message(damagee, getClassType().getName(), UtilMessage.deserialize("<alt2>%s</alt2> hit you with <green>%s %s</green>.", damager.getName(), getName(), level));
            }
        }
    }

    @Override
    public void displayTrail(Location location) {
        Particle.GLOW.builder().location(location).count(3).extra(0).receivers(60, true).spawn();
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

    public double getNaturalRegenerationDisabledDuration(int level) {
        return baseNaturalRegenerationDisabledDuration + ((level - 1) * increaseNaturalRegenerationDisabledDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        baseRegenerationStrength = getConfig("baseRegenerationStrength", 3, Integer.class);
        increaseRegenerationStrengthPerLevel = getConfig("increaseRegenerationStrengthPerLevel", 0, Integer.class);
        baseNaturalRegenerationDisabledDuration = getConfig("baseNaturalRegenerationDisabledDuration", 3.0, Double.class);
        increaseNaturalRegenerationDisabledDurationPerLevel = getConfig("increaseNaturalRegenerationDisabledDurationPerLevel", 0.5, Double.class);
    }
}
