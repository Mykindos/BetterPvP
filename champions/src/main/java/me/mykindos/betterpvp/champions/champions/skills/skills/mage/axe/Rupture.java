package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Particle; // For spawning particles

import java.util.ArrayList;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Rupture extends Skill implements Listener, InteractSkill, CooldownSkill, AreaOfEffectSkill, DebuffSkill, DamageSkill {

    private final WeakHashMap<Player, ArrayList<LivingEntity>> cooldownJump = new WeakHashMap<>();
    private final WeakHashMap<ArmorStand, Long> stands = new WeakHashMap<>();

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double baseSlowDuration;

    private double slowDurationIncreasePerLevel;

    private int slowStrength;

    @Inject
    public Rupture(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Rupture";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Rupture the earth in the direction",
                "you are facing, dealing " + getValueString(this::getDamage, level) + " damage,",
                "knocking up and giving <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> to enemies",
                "hit for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
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
    public void onUpdate() {
        stands.entrySet().removeIf(entry -> {
            if (entry.getValue() - System.currentTimeMillis() <= 0) {
                entry.getKey().remove();
                return true;
            }
            return false;
        });
    }


    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1));
    }

    @Override
    public void activate(Player player, int level) {
        final Vector direction = player.getLocation().getDirection().normalize().multiply(0.5D); // Travel direction
        Location startLoc = player.getLocation().clone().add(0, 1, 0); // Start location
        double maxDistance = 20; // Maximum range of the ability
        final double[] traveledDistance = {0}; // Distance traveled

        // List to track hit entities and prevent multiple hits
        ArrayList<LivingEntity> hitEntities = new ArrayList<>();

        // Particle-based traveling effect
        BukkitTask travelTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Move the location forward
                startLoc.add(direction);
                traveledDistance[0] += direction.length();

                // Check if the current block is solid
                Block currentBlock = startLoc.getBlock();
                if (UtilBlock.solid(currentBlock)) {
                    // Spawn particle simulating block breaking
                    player.getWorld().spawnParticle(
                            Particle.BLOCK,
                            startLoc.clone().add(0, 0.5, 0), // Center the particle
                            20, 0.3, 0.3, 0.3, 0.05,
                            currentBlock.getBlockData()
                    );
                }

                // Check for collision with entities
                for (LivingEntity entity : UtilEntity.getNearbyEnemies(player, startLoc, 1.5)) {
                    if (!hitEntities.contains(entity)) {
                        triggerExplosion(player, entity, startLoc, level);
                        hitEntities.add(entity);
                        cancel(); // Stop the particle travel
                        return;
                    }
                }

                // Stop the particle travel if maximum range is reached
                if (traveledDistance[0] >= maxDistance || !UtilBlock.airFoliage(currentBlock)) {
                    cancel();
                }
            }
        }.runTaskTimer(champions, 0, 2);
    }

    /**
     * Triggers an explosion of blocks and applies effects at the collision point.
     */
    private void triggerExplosion(Player player, LivingEntity target, Location explosionLocation, int level) {
        // Spawn block explosion effect
        for (int i = 0; i < 10; i++) {
            Location debrisLoc = explosionLocation.clone().add(
                    UtilMath.randDouble(-1, 1),
                    UtilMath.randDouble(0, 1),
                    UtilMath.randDouble(-1, 1)
            );

            player.getWorld().spawnParticle(
                    Particle.BLOCK,
                    debrisLoc, 10, 0.2, 0.2, 0.2,
                    explosionLocation.getBlock().getBlockData()
            );
        }

        // Apply knockback
        Vector knockbackVector = target.getLocation().toVector()
                .subtract(player.getLocation().toVector())
                .normalize()
                .multiply(1.0);
        target.setVelocity(knockbackVector);

        // Apply damage
        UtilDamage.doCustomDamage(new CustomDamageEvent(
                target, player, null, DamageCause.CUSTOM, getDamage(level), false, getName()
        ));

        // Apply slowness effect
        championsManager.getEffects().addEffect(
                target, player, EffectTypes.SLOWNESS,
                slowStrength,
                (long) (getSlowDuration(level) * 1000L)
        );
    }

    private Block getNearestSolidBlock(Location location) {
        for (int y = 0; y < location.getY(); y++) {
            Block block = location.clone().subtract(0, y, 0).getBlock();
            if (!UtilBlock.airFoliage(block) && UtilBlock.solid(block)) {
                return block;
            }
        }
        return null;
    }
    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 8.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseSlowDuration = getConfig("baseSlowDuration", 1.5, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);
    }
}
