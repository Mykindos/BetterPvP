package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class Upheaval extends Skill implements InteractSkill, CooldownSkill, AreaOfEffectSkill, DebuffSkill, DamageSkill, CrowdControlSkill {

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private int slowStrength;
    private double baseDistance;
    private double distanceIncreasePerLevel;
    private double pullStrength;
    private double minRockHeight;
    private double maxRockHeight;
    private double heightRamp;
    private double rockScale;
    private int rocksPerStep;
    private double stepDistance;
    private int stepsPerTick;
    private double spread;
    private double hitRadius;
    private int lifespanTicks;
    private int sinkTicks;

    @Inject
    public Upheaval(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Upheaval";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component distance = getValueComponent(this::getDistance, level, 1);
        Component slowDuration = getValueComponent(this::getSlowDuration, level);
        Component slowness = Translations.component("champions.skill.effect.slowness",
                Component.text(UtilFormat.getRomanNumeral(slowStrength))).color(NamedTextColor.WHITE);
        Component cooldown = getValueComponent(this::getCooldown, level);
        return Translations.componentLines("champions.skill.knight.upheaval.description",
                distance, damage, slowness, slowDuration, cooldown);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    public double getDistance(int level) {
        return baseDistance + ((level - 1) * distanceIncreasePerLevel);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public boolean activate(Player player, int level) {
        final Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) {
            return false;
        }
        direction.normalize();
        final Vector lateral = new Vector(-direction.getZ(), 0, direction.getX());

        final Location origin = player.getLocation();
        final double maxDistance = getDistance(level);
        final Set<UUID> caught = new HashSet<>();

        new SoundEffect("minecraft", "item.mace.smash_ground_heavy", 0.8f).play(origin);

        new BukkitRunnable() {

            private double travelled = stepDistance;

            @Override
            public void run() {
                for (int tickStep = 0; tickStep < stepsPerTick; tickStep++) {
                    if (travelled > maxDistance || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    final Location step = origin.clone().add(direction.clone().multiply(travelled));
                    final Optional<Location> ground = surfaceAt(step, origin.getY());
                    if (ground.isEmpty()) {
                        cancel();
                        return;
                    }

                    final double progress = travelled / maxDistance;
                    final double height = minRockHeight + (maxRockHeight - minRockHeight) * Math.pow(progress, heightRamp);

                    for (int i = 0; i < rocksPerStep; i++) {
                        final Location offset = step.clone().add(lateral.clone().multiply(UtilMath.randDouble(-spread, spread)));
                        surfaceAt(offset, origin.getY()).ifPresent(surface -> erupt(surface, height));
                    }

                    if (tickStep == 0) { // One crunch per tick, however much ground it covers
                        final Location impact = ground.get();
                        new SoundEffect(impact.getBlock().getBlockSoundGroup().getBreakSound(), 0.6f, 0.7f).play(impact);
                    }

                    for (LivingEntity enemy : UtilEntity.getNearbyEnemies(player, step, hitRadius)) {
                        if (!caught.add(enemy.getUniqueId())) {
                            continue;
                        }
                        heave(player, enemy, level);
                    }

                    travelled += stepDistance;
                }
            }
        }.runTaskTimer(champions, 0L, 1L);

        return true;
    }

    /**
     * Yanks the enemy off their feet and back toward the caster, so the ground smash reads as a summons
     * rather than a shove.
     */
    private void heave(Player player, LivingEntity enemy, int level) {
        Vector pull = player.getLocation().toVector().subtract(enemy.getLocation().toVector()).setY(0);
        if (pull.lengthSquared() < 0.0001) {
            pull = player.getLocation().getDirection().setY(0).multiply(-1);
        }
        pull.normalize();

        final VelocityData velocityData = new VelocityData(pull, pullStrength, false, 0.0, 0.45, 0.7, true);
        UtilVelocity.velocity(enemy, player, velocityData, VelocityType.CUSTOM);

        championsManager.getEffects().addEffect(enemy, player, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000L));

        final DamageEvent damageEvent = new DamageEvent(enemy, player, null, new SkillDamageCause(this), getDamage(level), getName());
        damageEvent.setKnockback(false); // Outward knockback would undo the pull
        UtilDamage.doDamage(damageEvent);
    }

    private Optional<Location> surfaceAt(Location location, double originY) {
        final Location probe = location.clone();
        probe.setY(originY + 1);
        final Optional<Location> surface = UtilLocation.getClosestSurfaceBelow(probe, 3.0);
        if (surface.isEmpty()) {
            return Optional.empty();
        }

        final Location result = surface.get();
        final double top = Math.floor(result.getY()) + 1;
        if (top - originY > 2.25) {
            return Optional.empty(); // Ran into a wall rather than a step
        }

        result.setY(top);
        return Optional.of(result);
    }

    /**
     * Raises a column of debris whose top sits {@code height} blocks above the surface. A single display is
     * only one block tall, so anything taller is built by stacking cubes downward from that top - the lowest
     * one ends up partially buried, which hides the seam with the ground.
     */
    private void erupt(Location surface, double height) {
        final Block block = surface.clone().subtract(0, 0.5, 0).getBlock();
        final BlockData data = block.getBlockData();
        final double shardHeight = rockScale * 1.5;
        final boolean shard = height >= shardHeight;
        final double stacked = shard ? height - shardHeight : height;
        final int cubes = Math.max(shard ? 0 : 1, (int) Math.ceil(stacked / rockScale));

        for (int i = 0; i < cubes; i++) {
            final Location centre = surface.clone().add(0, stacked - rockScale * (i + 0.5), 0);
            final Vector3f scale = new Vector3f(
                    (float) (rockScale * UtilMath.randDouble(0.85, 1.15)),
                    (float) rockScale,
                    (float) (rockScale * UtilMath.randDouble(0.85, 1.15)));
            spawnRock(centre, data, scale, tumble(35));
        }

        if (shard) {
            final Location tip = surface.clone().add(0, stacked + shardHeight / 2, 0);
            final float width = (float) (rockScale * 0.45);
            spawnRock(tip, data, new Vector3f(width, (float) shardHeight, width), tilt());
        }

        Particle.BLOCK.builder()
                .location(surface.clone().add(0, 0.2, 0))
                .data(data)
                .offset(0.3, 0.2, 0.3)
                .count(12)
                .receivers(48)
                .spawn();
    }

    private BlockDisplay spawnRock(Location centre, BlockData data, Vector3f scale, Quaternionf rotation) {
        final BlockDisplay display = centre.getWorld().spawn(centre, BlockDisplay.class, spawned -> {
            spawned.setBlock(data);
            spawned.setPersistent(false);

            // Blocks render from their corner, so the centring offset has to be rotated with them
            final Vector3f translation = rotation.transform(new Vector3f(-scale.x / 2f, -scale.y / 2f, -scale.z / 2f));
            spawned.setTransformation(new Transformation(translation, rotation, scale, new Quaternionf()));
        });

        UtilServer.runTaskLater(champions, () -> sink(display), lifespanTicks);
        return display;
    }

    private void sink(BlockDisplay display) {
        if (!display.isValid()) {
            return;
        }

        final Transformation transformation = display.getTransformation();
        transformation.getTranslation().add(0f, -2.5f, 0f);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(sinkTicks);
        display.setTransformation(transformation);

        UtilServer.runTaskLater(champions, display::remove, sinkTicks + 1L);
    }

    private Quaternionf tumble(double maxDegrees) {
        final Vector3f axis = new Vector3f(
                (float) UtilMath.randDouble(-1, 1),
                (float) UtilMath.randDouble(-1, 1),
                (float) UtilMath.randDouble(-1, 1));
        if (axis.lengthSquared() < 0.0001f) {
            axis.set(0f, 1f, 0f);
        }
        return new Quaternionf().fromAxisAngleRad(axis.normalize(), (float) Math.toRadians(UtilMath.randDouble(0, maxDegrees)));
    }

    private Quaternionf tilt() {
        final Vector3f axis = new Vector3f((float) UtilMath.randDouble(-1, 1), 0f, (float) UtilMath.randDouble(-1, 1));
        if (axis.lengthSquared() < 0.0001f) {
            axis.set(1f, 0f, 0f);
        }
        return new Quaternionf().fromAxisAngleRad(axis.normalize(), (float) Math.toRadians(UtilMath.randDouble(8, 26)));
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 6.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
        baseSlowDuration = getConfig("baseSlowDuration", 1.5, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.25, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);
        baseDistance = getConfig("baseDistance", 7.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.5, Double.class);
        pullStrength = getConfig("pullStrength", 0.9, Double.class);
        minRockHeight = getConfig("minRockHeight", 0.5, Double.class);
        maxRockHeight = getConfig("maxRockHeight", 1.5, Double.class);
        heightRamp = getConfig("heightRamp", 2.5, Double.class);
        rockScale = getConfig("rockScale", 0.6, Double.class);
        rocksPerStep = getConfig("rocksPerStep", 2, Integer.class);
        stepDistance = getConfig("stepDistance", 0.6, Double.class);
        stepsPerTick = getConfig("stepsPerTick", 3, Integer.class);
        spread = getConfig("spread", 0.9, Double.class);
        hitRadius = getConfig("hitRadius", 1.8, Double.class);
        lifespanTicks = getConfig("lifespanTicks", 50, Integer.class);
        sinkTicks = getConfig("sinkTicks", 8, Integer.class);
    }
}
