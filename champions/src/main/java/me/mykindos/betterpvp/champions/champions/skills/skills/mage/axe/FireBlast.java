package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;


import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.FireSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@BPvPListener
public class FireBlast extends Skill implements InteractSkill, CooldownSkill, Listener, FireSkill, CrowdControlSkill, OffensiveSkill {

    private double speed;
    public final List<LargeFireball> fireballs = new ArrayList<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseFireDuration;
    private double fireDurationIncreasePerLevel;
    private double radius;
    private double radiusIncreasePerLevel;
    private double minFireDuration;
    private double minFireDurationIncreasePerLevel;


    @Inject
    public FireBlast(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fire Blast";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Launch a fireball which explodes on impact,",
                "knocking back any players within <val>" + getRadius(level) + "</val> blocks",
                "and igniting them for up to <val>" + getFireDuration(level) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getFireDuration(int level) {
        return baseFireDuration + ((level - 1) * fireDurationIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);

    }

    public double getMinFireDuration(int level) {
        return minFireDuration + ((level - 1) * minFireDurationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @UpdateEvent
    public void update() {
        Iterator<LargeFireball> it = fireballs.iterator();
        while (it.hasNext()) {
            LargeFireball fireball = it.next();
            if (fireball == null || fireball.isDead()) {
                it.remove();
                continue;
            }
            if (fireball.getLocation().getY() < 255 || !fireball.isDead()) {
                Particle.LAVA.builder().location(fireball.getLocation()).receivers(30).count(1).spawn();
            } else {
                it.remove();
            }
        }
    }

    // If damaged by entity do nothing
    @EventHandler
    public void onDeflect(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LargeFireball fireball) {
            if (fireball.getShooter() instanceof Player){
                if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);

            if (!(largeFireball.getShooter() instanceof Player)) {
                return;
            }

            Player shooter = (Player) largeFireball.getShooter();
            int level = getLevel(shooter);
            if (level < 1) {
                return;
            }

            final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(shooter, largeFireball.getLocation(), getRadius(level), EntityProperty.ALL);

            new ParticleBuilder(Particle.EXPLOSION)
                    .location(largeFireball.getLocation())
                    .count(1)
                    .receivers(60)
                    .spawn();

            double radius = getRadius(level);
            if (shooter.getLocation().distance(largeFireball.getLocation()) <= radius && nearby.stream().noneMatch(entry -> entry.get().equals(shooter))) {
                nearby.add(new KeyValue<>(shooter, EntityProperty.FRIENDLY));
            }

            for (KeyValue<LivingEntity, EntityProperty> entry : nearby) {
                EntityProperty property = entry.getValue();
                final LivingEntity target = entry.get();

                Vector explosionToTarget = target.getLocation().toVector().subtract(largeFireball.getLocation().toVector());
                double distance = explosionToTarget.length();
                explosionToTarget.normalize();

                double scalingFactor = 1 - (distance / radius);
                scalingFactor = Math.max(scalingFactor, 0);

                double yVelocity = 1.5D;
                double scaledYVelocity = scalingFactor * yVelocity;

                explosionToTarget.multiply(scalingFactor * 0.5D);
                explosionToTarget.setY(scaledYVelocity);

                VelocityData velocityData = new VelocityData(explosionToTarget, scalingFactor * 0.5D, false, 0.0D, scalingFactor * 1.2D, scalingFactor * 2.0D, false);
                UtilVelocity.velocity(target, shooter, velocityData);

                double fireDuration = getMinFireDuration(level) + (scalingFactor * (getFireDuration(level) - getMinFireDuration(level)));
                if (property == EntityProperty.ENEMY) {
                    UtilServer.runTaskLater(champions, () -> target.setFireTicks((int) (20 * fireDuration)), 2);

                }
            }
        }
    }


    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof LargeFireball largeFireball) {
            fireballs.remove(largeFireball);
            largeFireball.getWorld().playSound(largeFireball.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.0f);
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getProjectile() != null) {
            Projectile fireball = event.getProjectile();
            if (fireball instanceof LargeFireball && fireball.getShooter() instanceof Player) {
                event.addReason(getName());
            }
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        LargeFireball fireball = player.launchProjectile(LargeFireball.class, player.getLocation().getDirection().multiply(speed));
        fireball.setYield(0);
        fireball.setIsIncendiary(false);

        fireballs.add(fireball);
    }

    @Override
    public void loadSkillConfig() {
        speed = getConfig("speed", 0.15, Double.class);
        baseDamage = getConfig("baseDamage", 6.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseFireDuration = getConfig("baseFireDuration", 1.0, Double.class);
        fireDurationIncreasePerLevel = getConfig("fireDurationIncreasePerLevel", 0.5, Double.class);
        radius = getConfig("radius", 6.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        minFireDuration = getConfig("minFireDuration", 2.0, Double.class);
        minFireDurationIncreasePerLevel = getConfig("minFireDurationIncreasePerLevel", 0.5, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
