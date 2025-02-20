package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


@Singleton
@BPvPListener
public class BioticQuiver extends Skill implements PassiveSkill, CooldownSkill, HealthSkill, TeamSkill, BuffSkill, DebuffSkill {

    private double baseFriendlyHealthRestoredOnHit;
    private double friendlyHealthRestoredOnHitIncreasedPerLevel;
    private double baseNaturalRegenerationDisabledDuration;
    private double increaseNaturalRegenerationDisabledDurationPerLevel;
    private final List<Arrow> arrows = new ArrayList<>();
    private final WeakHashMap<Player, Arrow> upwardsArrows = new WeakHashMap<>();
    private final WeakHashMap<Arrow, Vector> initialVelocities = new WeakHashMap<>();

    @Inject
    public BioticQuiver(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Biotic Quiver";
    }

    /*
    BEFORE:
    - shoot allies cleanses negative effects
    - Heals them with regen 3
    - shoot enemies give antiheal

     */
    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Shooting yourself or an ally with an arrow",
                "instantly restores " + getValueString(this::getFriendlyHealthRestoredOnHit, level) + " health.",
                "",
                "Shooting an enemy with an arrow",
                "gives them <effect>Anti Heal</effect> for " + getValueString(this::getNaturalRegenerationDisabledDuration, level) + " seconds",
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
        return SkillType.PASSIVE_A;
    }

    // Figure out how to track the arrows
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreDamageEvent(PreCustomDamageEvent event) {
        CustomDamageEvent cde = event.getCustomDamageEvent();
        if (!(cde.getProjectile() instanceof Arrow arrow)) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;

        upwardsArrows.remove(damager);

        int level = getLevel(damager);
        if (level > 0) {
            onHit(damager, cde.getDamagee(), level);
            arrows.remove(arrow);
            arrow.remove();
            cde.addReason(getName());
            if (UtilEntity.isEntityFriendly(damager, cde.getDamagee())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onArrowShoot(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        if (getLevel(shooter) <= 0) return;

        arrows.add(arrow);
    }

    /**
     * Handles adding arrows to the upwardsArrows collection.
     * UpwardsArrows is used to modify the hitbox of the arrows.
     */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            Vector initialVelocity = arrow.getVelocity();
            int level = getLevel(shooter);

            double totalMagnitude = initialVelocity.length();

            if (level > 0 && initialVelocity.getY() / totalMagnitude >= 0.5) {
                upwardsArrows.put(shooter, arrow);
                initialVelocities.put(arrow, initialVelocity);
            }
        }
    }


    @UpdateEvent
    public void checkPlayerHitboxes() {
        Iterator<Map.Entry<Player, Arrow>> iterator = upwardsArrows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Arrow> entry = iterator.next();
            Player shooter = entry.getKey();
            Arrow arrow = entry.getValue();
            if (!arrows.contains(arrow)) continue;

            Vector initialVelocity = initialVelocities.get(arrow);

            if (arrow.getVelocity().getY() < 0 && initialVelocity != null && initialVelocity.length() < 0.5) {

                RayTraceResult result = arrow.getWorld().rayTraceEntities(
                        arrow.getLocation(),
                        arrow.getLocation().getDirection(),
                        0.5,
                        0.2,
                        entity -> entity instanceof LivingEntity
                );

                if (result != null && result.getHitEntity() != null && result.getHitEntity().equals(shooter)) {
                    Player target = (Player) result.getHitEntity();
                    int level = getLevel(shooter);
                    onHit(target, target, level);
                    iterator.remove();
                    initialVelocities.remove(arrow);
                    upwardsArrows.remove(shooter);
                    arrow.remove();
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        Arrow upwardsArrow = upwardsArrows.get(player);
        if (upwardsArrow == null) return;
        if (!upwardsArrow.equals(event.getEntity())) return;

        upwardsArrows.remove(player);
    }

    public void onHit(Player damager, LivingEntity target, int level) {
        if (championsManager.getCooldowns().hasCooldown(damager, getName())) return;

        championsManager.getCooldowns().use(damager, getName(), getCooldown(level), false, true, isCancellable());

        if (UtilEntity.isEntityFriendly(damager, target)) {
            UtilPlayer.health((Player) target, getFriendlyHealthRestoredOnHit(level));

            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 1.5F);

            UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <yellow>%s</yellow> with <green>%s %s</green>", target.getName(), getName(), level));
            if (!damager.equals(target)) {
                UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("You were hit by <yellow>%s</yellow> with <green>%s %s</green>", damager.getName(), getName(), level));
            }

        } else {
            championsManager.getEffects().addEffect(target, damager, EffectTypes.ANTI_HEAL, 1, (long) (getNaturalRegenerationDisabledDuration(level) * 1000));
            UtilMessage.message(damager, getClassType().getName(), UtilMessage.deserialize("You hit <alt2>%s</alt2> with <green>%s %s</green>.", target.getName(), getName(), level));
            UtilMessage.message(target, getClassType().getName(), UtilMessage.deserialize("<alt2>%s</alt2> hit you with <green>%s %s</green>.", damager.getName(), getName(), level));
        }

    }

    // follow harrows logic
    public void displayTrail(Location location) {
        Particle.GLOW.builder().location(location).count(3).extra(0).receivers(60, true).spawn();
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getFriendlyHealthRestoredOnHit(int level) {
        return baseFriendlyHealthRestoredOnHit + ((level - 1) * friendlyHealthRestoredOnHitIncreasedPerLevel);
    }

    public double getNaturalRegenerationDisabledDuration(int level) {
        return baseNaturalRegenerationDisabledDuration + ((level - 1) * increaseNaturalRegenerationDisabledDurationPerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseFriendlyHealthRestoredOnHit = getConfig("baseFriendlyHealthRestoredOnHit", 1.0, Double.class);
        friendlyHealthRestoredOnHitIncreasedPerLevel = getConfig("friendlyHealthRestoredOnHitIncreasedPerLevel", 0.25, Double.class);
        baseNaturalRegenerationDisabledDuration = getConfig("baseNaturalRegenerationDisabledDuration", 3.0, Double.class);
        increaseNaturalRegenerationDisabledDurationPerLevel = getConfig("increaseNaturalRegenerationDisabledDurationPerLevel", 0.5, Double.class);
    }
}