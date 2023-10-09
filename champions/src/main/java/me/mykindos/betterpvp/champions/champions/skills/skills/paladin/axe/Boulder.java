package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.axe;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.paladin.data.BoulderObject;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.math.Function3d;
import me.mykindos.betterpvp.core.utilities.math.VectorParabola;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@BPvPListener
public class Boulder extends Skill implements Listener, InteractSkill, CooldownSkill {

    private final WeakHashMap<Player, List<BoulderObject>> boulders = new WeakHashMap<>();

    private double baseHeal;
    private double baseDamage;
    private double radius;

    @Inject
    public Boulder(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Boulder";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Right click with an Axe to activate",
                "",
                "Throw a boulder forward that",
                "deals <val>" + getDamage(level) + "</val> damage to all nearby",
                "enemies and heals surrounding allies for",
                "<val>" + getHeal(level) + "</val> health in a radius of <val>" + getRadius(level) + "</val> blocks.",
                "",
                "Recharge: <val>" + getCooldown(level)
        };
    }

    private double getRadius(int level) {
        return radius + 0.5 * (level - 1);
    }

    private double getDamage(int level) {
        return baseDamage + (level - 1);
    }

    private double getHeal(int level) {
        return baseHeal + (level - 1);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d);
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public void activate(Player player, int level) {
        final int tickInterval = 5;
        final double blocksPerCycle = 10 * tickInterval / 20d;
        final double maxDistance = 50;
        final int maxCycles = (int) (maxDistance / blocksPerCycle);

        final Location loc = player.getEyeLocation();
        final double pitch = loc.getPitch();
        final double offset = pitch / 5;
        final Function3d parabola = new VectorParabola(x -> (-Math.pow(x, 2) / 150) * Math.sqrt(Math.abs(pitch) + 1));
        final Vector direction = loc.getDirection().setY(0).normalize().setY(1);
        final Vector start = Vector.fromJOML(parabola.atDistance(offset)).multiply(direction);
        loc.subtract(start); // start compensation

        List<Vector3f> transformations = new ArrayList<>();
        for (int cycle = 0; cycle < maxCycles; cycle++) {
            double distance = cycle * blocksPerCycle;
            final Vector3d vec = parabola.atDistance(distance + offset);
            final Vector vector = Vector.fromJOML(vec);
            final Vector multiply = vector.multiply(direction);
            transformations.add(multiply.toVector3f());
        }

        List<Location> particleLocs = new ArrayList<>();
        for (double distance = 0; distance <= maxDistance; distance += 0.1) {
            final Vector3d vec = parabola.atDistance(distance + offset);
            final Vector vector = Vector.fromJOML(vec);
            final Vector multiply = vector.multiply(direction);
            final Location particleLoc = multiply.toLocation(loc.getWorld()).add(loc);
            particleLocs.add(particleLoc);
        }

        // Reset rotation for block displays
        loc.setYaw(0);
        loc.setPitch(0);
        // Center this location
//        loc.subtract(0.5, 0.5, 0.5);

        BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class);
        display.setGlowing(true);
        display.setGlowColorOverride(Color.RED);
        display.setBlock(Bukkit.createBlockData(Material.OBSIDIAN));
        final Iterator<Vector3f> iterator = transformations.iterator();
        AtomicInteger cycles = new AtomicInteger(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cycles.incrementAndGet() > maxCycles) {
                    display.remove();
                    this.cancel();
                    return;
                }

                if (iterator.hasNext()) {
                    final Vector3f translation = iterator.next();
                    final float angle = (float) Math.toRadians(45);

                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(tickInterval);
                    display.setTransformation(new Transformation(new Vector3f(),
                            new AxisAngle4f(angle, 0, 1, 0),
                            display.getTransformation().getScale(),
                            new AxisAngle4f()));
                }

                for (Location location : particleLocs) {
                    Particle.REDSTONE.builder().color(255, 0, 0).location(location).receivers(60).spawn();
                }
            }
        }.runTaskTimer(champions, 0L, tickInterval);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseHeal = getConfig("baseHeal", 2.0, Double.class);
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        radius = getConfig("radius", 4.0, Double.class);
    }

    @UpdateEvent
    public void collisionCheck() {
        final Iterator<Map.Entry<Player, List<BoulderObject>>> iterator = boulders.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BoulderObject>> entry = iterator.next();
            final Player caster = entry.getKey();

            final ListIterator<BoulderObject> boulderEntries = entry.getValue().listIterator();
            while (boulderEntries.hasNext()) {
                final BoulderObject boulder = boulderEntries.next();
                if (UtilTime.elapsed(boulder.getCastTime(), 10_000)) { // expire after 10 secs
                    boulderEntries.remove();
                    boulder.despawn();
                    continue;
                }

                final ArmorStand referenceEntity = boulder.getReferenceEntity();
                if (UtilBlock.isGrounded(referenceEntity) || referenceEntity.wouldCollideUsing(boulder.getBoundingBox())) {
                    boulder.impact(caster);
                    boulder.despawn();
                    boulderEntries.remove();
                }
            }

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

}
