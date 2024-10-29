package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Singleton
@BPvPListener
public class WindBurst extends Skill implements InteractSkill, CooldownSkill, Listener, AreaOfEffectSkill, DamageSkill, CrowdControlSkill, MovementSkill {

    private double cooldownDecreasePerLevel;
    private double damageIncreasePerLevel;
    private double damage;
    private double radius;
    private double velocity;
    private double radiusIncreasePerLevel;
    private double particleSpeed;
    private int burstDuration;
    private double yMax;
    private double yAdd;
    private double selfVelocity;
    private double yAddSelf;
    private double yMaxSelf;
    private double fallDamageLimit;
    private double ySetSelf;
    private double ySet;
    private final TaskScheduler taskScheduler;

    private final Random random = new Random();

    @Inject
    public WindBurst(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Wind Burst";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Explode in a burst of wind, launching",
                "yourself upwards and pushing away",
                "enemeis within " + getValueString(this::getRadius, level) + " blocks and dealing",
                getValueString(this::getDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public void activate(Player player, int level) {
        windBurst(player, level);
    }

    private void windBurst(Player player, int level) {
        if (getLevel(player) <= 0) return;

        Location location = player.getLocation().add(0, 1, 0);
        double maxRadius = getRadius(level);

        List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, location, getRadius(level));
        Vector direction2 = player.getEyeLocation().getDirection();

        VelocityData selfVelocityData = new VelocityData(direction2, selfVelocity, false, ySetSelf, yAddSelf, yMaxSelf, false);
        UtilVelocity.velocity(player, player, selfVelocityData, VelocityType.CUSTOM);
        taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
            Player target = Bukkit.getPlayer(uuid);
            if(target != null) {
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                        50L, true, true, UtilBlock::isGrounded);
            }
        }, 1000));

        for (LivingEntity enemy : enemies) {
            double yTranslate = location.add(0, -1, 0).getY();
            Location enemyLocation = enemy.getLocation();
            enemyLocation.setY(yTranslate);
            Vector direction = enemyLocation.toVector().subtract(location.toVector()).normalize();
            VelocityData enemyVelocityData = new VelocityData(direction, velocity, false, ySet, yAdd, yMax, true);
            UtilVelocity.velocity(enemy, player, enemyVelocityData, VelocityType.CUSTOM);
            UtilDamage.doCustomDamage(new CustomDamageEvent(enemy, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Wind Burst"));
        }

        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > burstDuration) {
                    this.cancel();
                    return;
                }
                currentRadius = (maxRadius / burstDuration) * ticks;

                spawnParticles(location, currentRadius);
                ticks++;
            }
        }.runTaskTimer(this.champions, 0, 1);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f);
    }



    private void spawnParticles(Location center, double radius) {
        int numParticles = 10;

        for (int i = 0; i < numParticles; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);

            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);

            double x = center.getX() + (radius * sinPhi * cosTheta);
            double y = center.getY() + (radius * sinPhi * sinTheta);
            double z = center.getZ() + (radius * cosPhi);

            Particle particle = (random.nextInt(3) < 2) ? Particle.CLOUD : Particle.GUST;
            center.getWorld().spawnParticle(particle, new Location(center.getWorld(), x, y, z), 0, 0, 0, 0, particleSpeed);
        }
    }


    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 2.0, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
        velocity = getConfig("velocity", 1.0, Double.class);
        selfVelocity = getConfig("selfVelocity", 0.5, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.0, Double.class);
        particleSpeed = getConfig("particleSpeed", 0.0, Double.class);
        burstDuration = getConfig("burstDuration", 5, Integer.class);
        yAdd = getConfig("yAdd", 0.6, Double.class);
        yMax = getConfig("yMax", 0.8, Double.class);
        yAddSelf = getConfig("yAddSelf", 0.8, Double.class);
        yMaxSelf = getConfig("yMaxSelf", 0.8, Double.class);
        fallDamageLimit = getConfig("fallDamageLimit", 20.0, Double.class);
        ySetSelf = getConfig("ySetSelf", 1.0, Double.class);
        ySet = getConfig("ySet", 0.0, Double.class);
    }
}