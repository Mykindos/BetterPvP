package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.GraspProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Grasp extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, CrowdControlSkill, DamageSkill {

    private final Map<Player, GraspProjectile> projectiles = new WeakHashMap<>();

    @Getter
    private double distance;
    @Getter
    private double damage;
    @Getter
    private double speed;

    @Inject
    public Grasp(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Grasp";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Create a wall of skulls that closes in on",
                "you from <val>" + getDistance() + "</val> blocks away, dragging along",
                "all enemies and dealing <val>" + getDamage() + "</val> damage",
                "",
                "Cooldown: <val>" + getCooldown()

        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @UpdateEvent
    public void onUpdate() {
        final Iterator<Map.Entry<Player, GraspProjectile>> iterator = projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, GraspProjectile> next = iterator.next();
            final GraspProjectile projectile = next.getValue();
            if (next.getKey() == null || !next.getKey().isOnline()) {
                projectile.remove();
                iterator.remove();
                continue;
            }

            projectile.tick();
            if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                projectile.remove();
                iterator.remove();
            }
        }
    }


    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public void activate(Player player) {
        final GraspProjectile removed = projectiles.remove(player);
        if (removed != null) {
            removed.remove();
        }

        Block block = player.getTargetBlock(null, (int) getDistance());
        final Location targetLocation = player.getEyeLocation();
        final Location startLocation = block.getLocation();
        final Vector direction = targetLocation.toVector().subtract(startLocation.toVector());
        long aliveMillis = (long) ((getDistance() / getSpeed()) * 1000);

        final GraspProjectile projectile = new GraspProjectile(player, 2.0, block.getLocation(), aliveMillis, Material.WITHER_SKELETON_SKULL, entity -> {
            UtilDamage.doCustomDamage(new CustomDamageEvent(entity, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName()));
            VelocityData velocityData = new VelocityData(direction.clone().normalize(), 1.6, false, 0, 0.5, 0.6, true);
            UtilVelocity.velocity(entity, player, velocityData);
        });
        projectile.redirect(direction.normalize().multiply(getSpeed()));
        projectiles.put(player, projectile);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        distance = getConfig("distance", 10.0, Double.class);
        damage = getConfig("damage", 260, Double.class);
        speed = getConfig("speed", 20.0, Double.class);
    }
}
