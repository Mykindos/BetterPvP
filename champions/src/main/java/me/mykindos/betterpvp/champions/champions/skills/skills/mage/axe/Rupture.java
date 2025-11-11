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
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Rupture extends Skill implements Listener, InteractSkill, CooldownSkill, AreaOfEffectSkill, DebuffSkill, DamageSkill {

    private final WeakHashMap<Player, ArrayList<LivingEntity>> cooldownJump = new WeakHashMap<>();
    private final WeakHashMap<BlockDisplay, Long> displays = new WeakHashMap<>();

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
        displays.entrySet().removeIf(entry -> {
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
        // calculate it from player yaw
        final double yaw = Math.toRadians(player.getLocation().getYaw() + 90.0F);
        final Vector vector = new Vector(Math.cos(yaw), 0, Math.sin(yaw)).normalize().multiply(0.6D);
        final Location loc = player.getLocation().subtract(0.0D, 1.0D, 0.0D).add(vector);
        loc.setY(Math.floor(loc.getY()));
        cooldownJump.put(player, new ArrayList<>());
        final BukkitTask runnable = new BukkitRunnable() {

            @Override
            public void run() {

                if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {
                    loc.add(0.0D, 1.0D, 0.0D);
                }

                if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {
                    cancel();
                    return;
                }

                if (loc.getBlock().getType().name().contains("DOOR")) {
                    cancel();
                    return;
                }

                if ((loc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType() == Material.AIR)) {
                    Block halfBlock = loc.clone().add(0, -0.5, 0).getBlock();
                    if (!halfBlock.getType().name().contains("SLAB") && !halfBlock.getType().name().contains("STAIR")) {
                        loc.add(0.0D, -1.0D, 0.0D);
                    }
                }

                loc.add(vector);
                for (int i = 0; i < 3; i++) {
                    Location tempLoc = new Location(player.getWorld(), loc.getX() + UtilMath.randDouble(-1.5D, 1.5D), loc.getY() + UtilMath.randDouble(0.3D, 0.8D) - 0.75,
                            loc.getZ() + UtilMath.randDouble(-1.5D, 1.5D));

                    Block nearestSolidBlock = getNearestSolidBlock(loc);
                    if (nearestSolidBlock == null) {
                        cancel();
                        return;
                    }

                    BlockDisplay display = loc.getWorld().spawn(tempLoc.clone().add(0, 1.0, 0), BlockDisplay.class, spawned -> {
                        spawned.setBlock(nearestSolidBlock.getBlockData());
                        spawned.setPersistent(false);
                        float angle = (float) Math.toRadians(UtilMath.randomInt(25));
                        spawned.setTransformation(new Transformation(
                                new Vector3f(-0.5f, -0.2f - 1, -0.5f),
                                new AxisAngle4f(angle, (float) Math.random(), (float) Math.random(), (float) Math.random()),
                                new Vector3f(0.6f, 0.6f, 0.6f),
                                new AxisAngle4f()
                        ));
                    });

                    new SoundEffect(nearestSolidBlock.getBlockSoundGroup().getBreakSound(), 0.8f, 1).play(display.getLocation());
                    Particle.BLOCK.builder()
                            .count(30)
                            .offset(0.5, 0.5, 0.5)
                            .location(tempLoc)
                            .data(nearestSolidBlock.getBlockData())
                            .receivers(60)
                            .spawn();

                    displays.put(display, System.currentTimeMillis() + 4000);

                    for (LivingEntity ent : UtilEntity.getNearbyEnemies(player, display.getLocation(), 1)) {

                        if (!cooldownJump.get(player).contains(ent)) {
                            VelocityData velocityData = new VelocityData(vector.clone(), 0.6, false, 0.0, 0.8, 2.0, false);
                            UtilVelocity.velocity(ent, player, velocityData, VelocityType.CUSTOM);

                            championsManager.getEffects().addEffect(ent, player, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000L));
                            UtilDamage.doDamage(new DamageEvent(ent, player, null, new SkillDamageCause(Rupture.this), getDamage(level), getName()));

                            cooldownJump.get(player).add(ent);
                        }
                    }
                }
            }
        }.runTaskTimer(champions, 0, 2);

        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.cancel();
                cooldownJump.get(player).clear();
            }
        }.runTaskLater(champions, 40);
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
