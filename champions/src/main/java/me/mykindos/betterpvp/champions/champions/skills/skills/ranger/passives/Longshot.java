package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Longshot extends Skill implements PassiveSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Projectile, Location> projectiles = new WeakHashMap<>();

    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;
    private double minDamage;
    private double maxDistance;
    private double deathMessageThreshold;

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "4.0")
    private double baseArrowDamage;

    @Inject
    public Longshot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Longshot";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your arrows start at " + getValueString(this::getMinDamage, level) + " damage but",
                "they gain extra damage the further",
                "they travel up to a maximum of " + getValueString(this::getMaxDamage, level),
                "damage at " + getValueString(this::getMaxDistance, level) + " blocks",
                "",
                "Cannot be used in own territory"};
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    public double getMinDamage(int level) {
        return minDamage;
    }

    public double getMaxDistance(int level){
        return maxDistance;
    }

    private boolean isValidProjectile(Projectile projectile) {
        return projectile instanceof Arrow || projectile instanceof Trident;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent(delay = 200)
    public void update() {
        Iterator<Projectile> it = projectiles.keySet().iterator();
        while (it.hasNext()) {
            Projectile next = it.next();
            if (next == null) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location location = next.getLocation().add(new Vector(0, 0.25, 0));
                Particle.FIREWORK.builder().location(location).receivers(60).extra(0).spawn();
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {
                projectiles.put(arrow, arrow.getLocation());
            }
        }
    }

    @UpdateEvent
    public void initializeTridents() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Trident trident : world.getEntitiesByClass(Trident.class)) {
                if (projectiles.containsKey(trident)) {
                    continue;
                }
                if (!(trident.getShooter() instanceof Player player)) {
                    continue;
                }

                int level = getLevel(player);
                if (level > 0) {
                    projectiles.put(trident, trident.getLocation());
                }
            }
        }
    }

    public static double horizontalOffset(Location loc1, Location loc2) {
        double deltaX = loc1.getX() - loc2.getX();
        double deltaZ = loc1.getZ() - loc2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isValidProjectile(projectile)) return;
        if (!projectiles.containsKey(projectile)) return;

        Location loc = projectiles.remove(projectile);
        int level = getLevel(damager);
        double distance = UtilMath.offset(loc, event.getDamagee().getLocation());
        double maxDamage = getMaxDamage(level);

        double distanceFactor = Math.min(distance / maxDistance, 1.0);
        double damageMultiplier = Math.pow(distanceFactor, 2);
        double scaledDamage = minDamage + (damageMultiplier * (maxDamage));

        if (scaledDamage > baseArrowDamage){
            UtilMessage.simpleMessage(damager, getClassType().getName(), "<alt>%s</alt> did <alt2>%.1f</alt2> damage.", getName(), scaledDamage);
        }

        event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_BREEZE_JUMP, (float)(2.0F * damageMultiplier), 1.5f);

        event.setDamage(scaledDamage);
        event.addReason(getName() + (distance > deathMessageThreshold ? " (" + (int) distance + " blocks)" : ""));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Projectile projectile) {
            if (event.getHitBlock() != null || event.getHitEntity() == null) {
                projectiles.entrySet().removeIf(entry -> entry.getKey().equals(projectile));
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxDamage = getConfig("baseMaxDamage", 14.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 3.0, Double.class);
        minDamage = getConfig("minDamage", 1.0, Double.class);
        maxDistance = getConfig("maxDistance", 64.0, Double.class);
        deathMessageThreshold = getConfig("deathMessageThreshold", 32.0, Double.class);
    }
}