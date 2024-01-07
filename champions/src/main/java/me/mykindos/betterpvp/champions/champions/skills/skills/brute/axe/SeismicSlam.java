package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class SeismicSlam extends Skill implements InteractSkill, CooldownSkill, Listener {

    private final Set<UUID> active = new HashSet<>();
    private final WeakHashMap<Player, Long> height = new WeakHashMap<>();
    private double radius;
    private double damage;
    public double damageIncreasePerLevel;
    public double cooldownDecreasePerLevel;

    @Inject
    public SeismicSlam(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Seismic Slam";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Leap up and slam into the ground, causing",
                "players within <stat>" + radius + "</stat> blocks to fly",
                "upwards and take <val>" + getSlamDamage(level) + "</val> damage",
                "",
                "Cooldown: " + getCooldown(level)
        };
    }

    public double getSlamDamage(int level){
        return damage + ((level-1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Get the block 0.25 blocks below the player
                Block blockBelow = player.getLocation().subtract(0, 0.75, 0).getBlock();

                if (player.isDead() || !UtilTime.elapsed(height.get(player), 500) || !UtilBlock.isGrounded(player) || !blockBelow.getType().isSolid()) {
                    continue;
                }

                slam(player);
                iterator.remove();
            } else {
                iterator.remove();
            }
        }
    }


    public void slam(final Player player) {
        active.remove(player.getUniqueId());

        int level = getLevel(player);
        List<LivingEntity> targets = UtilEntity.getNearbyEnemies(player, player.getLocation(), 5.5d + 0.5 * level);
        double baseVelocity = 0.6;
        double maxDistance = 5.5d + 0.5;

        for (LivingEntity target : targets) {
            if (target.equals(player)) {
                continue;
            }

            double distance = player.getLocation().distance(target.getLocation());
            double distanceFactor = 1 - (distance / maxDistance);
            distanceFactor = Math.max(0, Math.min(distanceFactor, 1));

            double scaledVelocity = baseVelocity + (2 * distanceFactor);
            Vector trajectory = UtilVelocity.getTrajectory2d(player.getLocation().toVector(), target.getLocation().toVector());
            UtilVelocity.velocity(target, trajectory, scaledVelocity, true, 0, 0.2 + 1.0 * distanceFactor, 1.4, true);

            double damage = calculateDamage(player, target);
            UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, damage, false, getName()));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2f, 0.2f);
        for (Block cur : UtilBlock.getInRadius(player.getLocation(), 4d).keySet()) {
            if (UtilBlock.airFoliage(cur.getRelative(BlockFace.UP)) && !UtilBlock.airFoliage(cur)) {
                cur.getWorld().playEffect(cur.getLocation(), Effect.STEP_SOUND, cur.getType().createBlockData());
            }
        }
    }

    public double calculateDamage(Player player, LivingEntity target) {
        int level = getLevel(player);
        double minDamage = getSlamDamage(level) / 4;
        double maxDistance = 5.5d + 0.5 * level;

        double distance = player.getLocation().distance(target.getLocation());
        double distanceFactor = 1 - (distance / maxDistance);
        distanceFactor = Math.max(0, Math.min(distanceFactor, 1));

        double scaledDamage = minDamage + (getSlamDamage(level) - minDamage) * distanceFactor;
        return scaledDamage;
    }

    @Override
    public SkillType getType() {

        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        Vector vec = player.getLocation().getDirection();
        if (vec.getY() < 0) {
            vec.setY(vec.getY() * -1);
        }
        UtilVelocity.velocity(player, vec, 0.6, false, 0, 0.8, 0.8, true);

        height.put(player, System.currentTimeMillis());
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        radius = getConfig("radius", 5.0, Double.class);
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 2.0, Double.class);
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 2.0, Double.class);
    }
}
