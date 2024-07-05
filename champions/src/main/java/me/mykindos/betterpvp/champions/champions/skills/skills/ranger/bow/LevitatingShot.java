package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class LevitatingShot extends PrepareArrowSkill implements OffensiveSkill, DebuffSkill {

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int levitationStrength;
    private double fallDamageLimit;
    private final Set<UUID> upwardsArrows = new HashSet<>();
    private final Map<UUID, LevitatedPlayerInfo> levitatedPlayers = new HashMap<>();

    @Inject
    public LevitatingShot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    private static class LevitatedPlayerInfo {
        UUID damagerUUID;
        long endTime;

        LevitatedPlayerInfo(UUID damagerUUID, long endTime) {
            this.damagerUUID = damagerUUID;
            this.endTime = endTime;
        }
    }

    @Override
    public String getName() {
        return "Levitating Shot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Your next arrow is tipped with mysterious magic causing",
                "the target to receive <effect>Levitation " + UtilFormat.getRomanNumeral(levitationStrength) + "</effect> for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "If teammates or yourself are hit by your levitation shot,",
                "crouching will cancel the effect",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.LEVITATION.getDescription(levitationStrength),
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
            onHit(damager, cde.getDamagee(), level);
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
            if (!upwardsArrows.remove(arrow.getUniqueId())) return;
            if (!arrows.contains(arrow)) return;

            Location arrowLocation = arrow.getLocation();
            int level = getLevel(shooter);
            if (level > 0) {
                for (Entity entity : arrowLocation.getWorld().getNearbyEntities(arrowLocation, 0.5, 0.5, 0.5)) {
                    if (entity instanceof Player && entity.getUniqueId().equals(shooter.getUniqueId())) {
                        Location playerLocation = entity.getLocation();
                        double distanceSquared = arrowLocation.distanceSquared(playerLocation);
                        double radiusSquared = 0.4 * 0.4;
                        if (distanceSquared <= radiusSquared) {
                            onHit(shooter, shooter, level);
                            return;
                        }
                    }
                }
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        long currentTime = System.currentTimeMillis();
        levitatedPlayers.entrySet().removeIf(entry -> {
            Player target = Bukkit.getPlayer(entry.getKey());
            LevitatedPlayerInfo info = entry.getValue();
            Player damager = Bukkit.getPlayer(info.damagerUUID);

            if (target != null && UtilEntity.isEntityFriendly(damager, target) && target.isSneaking()) {
                if (championsManager.getEffects().hasEffect(target, EffectTypes.LEVITATION)) {
                    championsManager.getEffects().removeEffect(target, EffectTypes.LEVITATION);
                    UtilMessage.message(target, getClassType().getName(), "You canceled the levitation effect by crouching.");
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        long endTime = System.currentTimeMillis() + (int) (getDuration(level) * 1000);
        levitatedPlayers.put(target.getUniqueId(), new LevitatedPlayerInfo(damager.getUniqueId(), endTime));
        championsManager.getEffects().addEffect(target, damager, EffectTypes.LEVITATION, levitationStrength, (int) (getDuration(level) * 1000));
        if(!damager.equals(target)){
            UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
        }
        UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>.", target.getName(), getName(), level));


        // Add fall resistance effect to the target if they are an ally
        if (UtilEntity.isEntityFriendly(damager, target)) {
            UtilServer.runTaskLater(champions, () -> {
                championsManager.getEffects().addEffect(target, target, EffectTypes.NO_FALL, getName(), (int) fallDamageLimit,
                        50L, true, true, UtilBlock::isGrounded);
            }, 3L);
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.SCULK_CHARGE_POP)
                .location(location)
                .count(1)
                .offset(0.1, 0.1, 0.1)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        levitationStrength = getConfig("levitationStrength", 4, Integer.class);
        fallDamageLimit = getConfig("fallDamageLimit", 10.0, Double.class);
    }
}
