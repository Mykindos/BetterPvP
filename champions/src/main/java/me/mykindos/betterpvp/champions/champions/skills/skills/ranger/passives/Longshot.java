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
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double deathMessageThreshold;
    private double minDamage;

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
                "Your arrows now deal damage that increases",
                "by " + getValueString(this::getDamage, level) + " damage per block it travels",
                "",
                "Your arrows start at " + getValueString(this::getMinDamage, level) + " damage",
                "and cap out at " + getValueString(this::getMaxDamage, level) + " damage",
                "",
                "Cannot be used in own territory"};
    }

    public double getMaxDamage(int level) {
        return minDamage + baseMaxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    public double getMinDamage(int level) {
        return minDamage;
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

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!arrows.containsKey(arrow)) return;

        Location loc = arrows.remove(arrow);
        int level = getLevel(damager);
        double length = UtilMath.offset(loc, event.getDamagee().getLocation());
        double damage = Math.min(getMaxDamage(level), length * getDamage(level));

        event.setDamage(minDamage + (damage));
        event.addReason(getName() + (length > deathMessageThreshold ? " (" + (int) length + " blocks)" : ""));

    }

    @EventHandler
    public void onDeath(CustomDeathEvent event) {

    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxDamage = getConfig("baseMaxDamage", 14.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 2.0, Double.class);
        baseDamage = getConfig("baseDamage", 0.4, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        minDamage = getConfig("minDamage", 1.0, Double.class);

        deathMessageThreshold = getConfig("deathMessageThreshold", 40.0, Double.class);
    }

}
