package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.GraspProjectile;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.LeechSoulProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Leech extends PrepareSkill implements CooldownSkill, HealthSkill, OffensiveSkill, DamageSkill {

    private final Map<Player, GraspProjectile> projectiles = new WeakHashMap<>();
    private final Map<Player, List<LeechSoulProjectile>> souls = new WeakHashMap<>();

    @Getter
    private double distance;
    @Getter
    private double damage;
    @Getter
    private double speed;
    @Getter
    private double health;

    @Inject
    public Leech(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Leech";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Create a wall of skulls that repels enemies",
                "and leeches health from them. The wall will",
                "continue for <val>" + getDistance() + "</val> blocks",
                "",
                "Deal <val>" + getDamage() + "</val> damage to enemies and heal for",
                "<val>" + getHealth() + "</val> health for each one.",
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
            if (next.getKey() == null || !next.getKey().isValid()) {
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

        final Iterator<Map.Entry<Player, List<LeechSoulProjectile>>> soulIterator = souls.entrySet().iterator();
        while (soulIterator.hasNext()) {
            final Map.Entry<Player, List<LeechSoulProjectile>> next = soulIterator.next();
            final List<LeechSoulProjectile> activeSouls = next.getValue();
            if (next.getKey() == null || !next.getKey().isValid() || activeSouls.isEmpty()) {
                activeSouls.clear();
                soulIterator.remove();
                continue;
            }

            final Iterator<LeechSoulProjectile> projectileIterator = activeSouls.iterator();
            while (projectileIterator.hasNext()) {
                final LeechSoulProjectile soul = projectileIterator.next();
                if (soul.isExpired() || soul.isMarkForRemoval()) {
                    projectileIterator.remove();
                    continue;
                }

                soul.tick();
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

        final Vector direction = player.getEyeLocation().getDirection();
        long aliveMillis = (long) ((getDistance() / getSpeed()) * 1000);

        final GraspProjectile projectile = new GraspProjectile(player, 2.0, player.getEyeLocation(), aliveMillis, Material.SKELETON_SKULL, entity -> {
            UtilDamage.doCustomDamage(new CustomDamageEvent(entity, player, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, getName()));
            VelocityData velocityData = new VelocityData(direction.clone().normalize(), 1.6, false, 0, 0.5, 0.6, true);
            UtilVelocity.velocity(entity, player, velocityData);

            final LeechSoulProjectile soul = new LeechSoulProjectile(player,
                    0.6,
                    entity.getLocation().add(0, entity.getHeight() / 2, 0),
                    10_000,
                    getSpeed(),
                    getHealth());
            soul.redirect(direction.clone());
            souls.computeIfAbsent(player, p -> new ArrayList<>()).add(soul);
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
        damage = getConfig("damage", 4.0, Double.class);
        speed = getConfig("speed", 20.0, Double.class);
        health = getConfig("health", 4.0, Double.class);
    }
}
