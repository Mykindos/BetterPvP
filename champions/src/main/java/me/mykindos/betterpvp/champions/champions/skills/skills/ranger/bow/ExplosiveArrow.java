package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.bow;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
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
import org.bukkit.util.Vector;

import java.util.List;

@Singleton
@BPvPListener
public class ExplosiveArrow extends PrepareArrowSkill {

    private double damage;
    private double damageIncreasePerLevel;
    private double radius;
    private double radiusIncreasePerLevel;

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
                "Shoot an explosive arrow that deals up to",
                "<stat>" + getDamage(level) + "</stat> damage to anything within <val>" + getRadius(level) + "</val> blocks",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int level) {
        return damage + level * damageIncreasePerLevel;
    }

    public double getRadius(int level){
        return radius + (radiusIncreasePerLevel * level);
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

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!arrows.contains(arrow)) return;
        if (!hasSkill(player)) return;

        Location arrowLocation = event.getEntity().getLocation();
        int level = getLevel(player);

        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(player, arrowLocation, getRadius(level), EntityProperty.ALL);

        player.getWorld().playSound(arrowLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        new ParticleBuilder(Particle.GUST_EMITTER)
                .location(arrowLocation)
                .count(1)
                .receivers(60)
                .spawn();

        double radius = getRadius(level);
        if (player.getLocation().distance(arrowLocation) <= radius && nearby.stream().noneMatch(entry -> entry.get().equals(player))) {
            nearby.add(new KeyValue<>(player, EntityProperty.FRIENDLY));
        }

        for (KeyValue<LivingEntity, EntityProperty> entry : nearby) {
            EntityProperty property = entry.getValue();
            final LivingEntity target = entry.get();

            Vector explosionToTarget = target.getLocation().toVector().subtract(arrowLocation.toVector());
            double distance = explosionToTarget.length();
            double scalingFactor = 1 - (distance / radius);
            scalingFactor = Math.max(scalingFactor, 0);

            double damage = getDamage(level);

            explosionToTarget.normalize();
            double yVelocity = 0.6D;
            double scaledYVelocity = scalingFactor * yVelocity;
            explosionToTarget.multiply(scalingFactor * 0.5D);
            explosionToTarget.setY(scaledYVelocity);

            VelocityData velocityData = new VelocityData(explosionToTarget, scalingFactor * 0.5D, false, 0.0D, scalingFactor * 1.2D, scalingFactor * 2.0D, false);
            UtilVelocity.velocity(target, player, velocityData);

            if (property == EntityProperty.ENEMY) {
                var cde = new CustomDamageEvent(target, null, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, "ExplosiveArrow");
                UtilDamage.doCustomDamage(cde);
            }
        }
    }

    @Override
    public void onHit(Player damager, LivingEntity target, int level) {
        // No implementation - ignore
    }

    @Override
    public void displayTrail(Location location) {
        new ParticleBuilder(Particle.GUST)
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
        damage = getConfig("damage", 10.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        radius = getConfig("radius", 4.5, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
    }
}
