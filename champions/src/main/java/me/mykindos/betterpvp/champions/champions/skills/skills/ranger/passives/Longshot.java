package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
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

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseDistance;
    private double distanceDecreasePerLevel;

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
                "Arrows that travel further than " + getValueString(this::getDistance, level),
                "Deal " + getValueString(this::getDamage, level) + " extra damage."
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public double getDistance(int level) {
        return baseDistance - (distanceDecreasePerLevel * (level - 1));
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(final DamageEvent event) {
        if (event.getProjectile() == null) return;
        final Projectile projectile = event.getProjectile();
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isValidProjectile(projectile)) return;
        if (!projectiles.containsKey(projectile)) return;

        final Location loc = projectiles.remove(projectile);
        final int level = getLevel(damager);
        final double distance = UtilMath.offset(loc, event.getDamagee().getLocation());
        if (distance < getDistance(level)) return;
        event.addModifier(new SkillDamageModifier.Flat(this, getDamage(level)));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (event.getHitBlock() != null || event.getHitEntity() == null) {
            projectiles.entrySet().removeIf(entry -> entry.getKey().equals(projectile));
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 2.5, Number.class).doubleValue();
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.75, Number.class).doubleValue();
        baseDistance = getConfig("baseDistance", 24.0, Number.class).doubleValue();
        distanceDecreasePerLevel = getConfig("distanceDecreasePerLevel", 0.0, Number.class).doubleValue();
    }
}