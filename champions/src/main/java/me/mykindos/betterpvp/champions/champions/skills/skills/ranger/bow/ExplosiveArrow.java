package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class ExplosiveArrow extends PrepareArrowSkill implements DamageSkill, OffensiveSkill, AreaOfEffectSkill, CrowdControlSkill {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double radiusIncreasePerLevel;
    private double radius;
    private double velocityMultiplier;
    private boolean groundBoost;
    private double yMax;
    private double yAdd;
    private final Map<UUID, Arrow> explosiveArrows = new HashMap<>();

    @Inject
    public ExplosiveArrow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Explosive Arrow";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Left click with a Bow to prepare",
                "",
                "Shoot an arrow full of gunpowder which will",
                "explode upon hitting the ground or an enemy,",
                "dealing " + getValueString(this::getDamage, level) + " damage to players within " + getValueString(this::getRadius, level) + " blocks",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level) + " seconds"
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    private void doExplosion(Player player, Location arrowLocation, int level) {
        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, arrowLocation, getRadius(level));

        for (LivingEntity enemy : enemies) {
            Vector direction = enemy.getLocation().toVector().subtract(arrowLocation.add(0, -2, 0).toVector()).normalize();
            VelocityData velocityData = new VelocityData(direction, velocityMultiplier, false, 0.0D, yAdd, yMax, groundBoost);
            UtilVelocity.velocity(enemy, player, velocityData, VelocityType.CUSTOM);
            UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Explosive Arrow"));
        }

        if (arrowLocation.distance(player.getLocation()) <= getRadius(level)) {
            Vector direction = player.getLocation().toVector().subtract(arrowLocation.add(0, -2, 0).toVector()).normalize();
            VelocityData velocityData = new VelocityData(direction, velocityMultiplier, false, 0.0D, yAdd, yMax, groundBoost);
            UtilVelocity.velocity(player, player, velocityData, VelocityType.CUSTOM);
        }

        Particle.EXPLOSION_EMITTER.builder()
                .location(arrowLocation)
                .receivers(60)
                .spawn();
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
        active.add(player.getUniqueId());
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        //ignore
    }

    @Override
    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        explosiveArrows.put(player.getUniqueId(), arrow);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!explosiveArrows.containsValue(arrow)) return;

        int level = getLevel(player);
        Location arrowLocation = arrow.getLocation();

        explosiveArrows.remove(player.getUniqueId());
        player.getWorld().playSound(arrowLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
        new BukkitRunnable() {
            @Override
            public void run() {
                doExplosion(player, arrowLocation, level);
            }
        }.runTaskLater(champions, 1L);

    }

    @UpdateEvent
    public void updateArrowTrail() {
        for (Arrow arrow : explosiveArrows.values()) {
            displayTrail(arrow.getLocation());
        }
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.POOF)
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
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        velocityMultiplier = getConfig("velocityMultiplier", 1.2, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.0, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
        groundBoost = getConfig("groundBoost", true, Boolean.class);
        yAdd = getConfig("yAdd", 0.8, Double.class);
        yMax = getConfig("yMax", 0.8, Double.class);
    }
}