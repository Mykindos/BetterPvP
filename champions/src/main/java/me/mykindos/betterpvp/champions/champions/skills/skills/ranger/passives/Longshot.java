package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    @Getter
    private double maxDamage;
    @Getter
    private double minDamage;
    @Getter
    private double maxDistance;
    private double deathMessageThreshold;

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "6.0")
    private double arrowDamage;

    @Inject
    public Longshot(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Longshot";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Your arrows start at <val>" + getMinDamage() + "</val> damage but",
                "they gain extra damage the further",
                "they travel up to a maximum of <val>" + getMaxDamage(),
                "damage at <val>" + getMaxDistance() + "</val> blocks",
                "",
                "Cannot be used in own territory"};
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

        if (hasSkill(player)) {
            PlayerCanUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerCanUseSkillEvent(player, this));
            if (!skillEvent.isCancelled()) {
                projectiles.put(arrow, arrow.getLocation());
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
        double distance = UtilMath.offset(loc, event.getDamagee().getLocation());
        double maxDamage = getMaxDamage();

        double distanceFactor = Math.min(distance / maxDistance, 1.0);
        double damageMultiplier = Math.pow(distanceFactor, 2);
        double scaledDamage = minDamage + (damageMultiplier * (maxDamage));

        if (scaledDamage > arrowDamage) {
            UtilMessage.simpleMessage(damager, getClassType().getName(), "<alt>%s</alt> did <alt2>%.1f</alt2> damage.", getName(), scaledDamage);
        }

        event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_BREEZE_JUMP, (float) (2.0F * damageMultiplier), 1.5f);

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
        maxDamage = getConfig("maxDamage", 13.0, Double.class);
        minDamage = getConfig("minDamage", 1.0, Double.class);
        maxDistance = getConfig("maxDistance", 64.0, Double.class);
        deathMessageThreshold = getConfig("deathMessageThreshold", 32.0, Double.class);
    }
}