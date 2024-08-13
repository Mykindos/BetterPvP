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
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Longshot extends Skill implements PassiveSkill, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Arrow, Location> arrows = new WeakHashMap<>();

    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;
    private double minDamage;
    private double maxDistance;
    private double deathMessageThreshold;

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

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent(delay = 200)
    public void update() {
        Iterator<Arrow> it = arrows.keySet().iterator();
        while (it.hasNext()) {
            Arrow next = it.next();
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
                arrows.put(arrow, arrow.getLocation());
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
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!arrows.containsKey(arrow)) return;

        Location loc = arrows.remove(arrow);
        int level = getLevel(damager);
        double distance = UtilMath.offset(loc, event.getDamagee().getLocation());
        double maxDamage = getMaxDamage(level);

        double distanceFactor = Math.min(distance / maxDistance, 1.0);
        double damageMultiplier = Math.pow(distanceFactor, 2);
        double scaledDamage = minDamage + (damageMultiplier * (maxDamage));

        event.getDamagee().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_BREEZE_JUMP, (float)(2.0F * damageMultiplier), 1.5f);

        event.setDamage(scaledDamage);
        event.addReason(getName() + (distance > deathMessageThreshold ? " (" + (int) distance + " blocks)" : ""));
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
