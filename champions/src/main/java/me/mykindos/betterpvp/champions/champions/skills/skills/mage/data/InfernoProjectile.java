package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public class InfernoProjectile extends Projectile {

    private final ItemDisplay display1;
    private final ItemDisplay display2;
    private final Supplier<Double> impactDamage;
    private final Supplier<Double> spreadDamage;
    private final Supplier<Integer> fireTicks;
    private final Supplier<Double> maxRadius;
    private final Supplier<Double> expansionRate;
    private final Skill skill;
    private final ChampionsManager championsManager;
    private long launchTime;
    private double displaySize;
    private double currentRadius = 0.0;
    private final Map<Location, Vector> expansionPoints = new HashMap<>();

    public InfernoProjectile(Player caster, Location location, Supplier<Double> impactDamage, Supplier<Double> spreadDamage,
                             Supplier<Integer> fireTicks, Supplier<Double> maxRadius, Supplier<Double> expansionRate,
                             double hitboxSize, double displaySize, long aliveTime, Skill skill, ChampionsManager championsManager) {
        super(caster, hitboxSize, location, aliveTime);
        this.impactDamage = impactDamage;
        this.spreadDamage = spreadDamage;
        this.fireTicks = fireTicks;
        this.maxRadius = maxRadius;
        this.expansionRate = expansionRate;
        this.skill = skill;
        this.championsManager = championsManager;
        this.displaySize = displaySize;

        // Create first netherrack display
        this.display1 = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(new ItemStack(Material.NETHERRACK));
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(2);
            spawned.setTeleportDuration(2);
        });

        // Create second netherrack display (opposite side)
        this.display2 = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(new ItemStack(Material.NETHERRACK));
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(2);
            spawned.setTeleportDuration(2);
        });
    }

    @Override
    public boolean isExpired() {
        return this.impacted
                ? expansionPoints.isEmpty()
                : launchTime != 0 && UtilTime.elapsed(launchTime, aliveTime);
    }

    public void setDisplaySize(double displaySize) {
        this.displaySize = displaySize;

        Transformation transformation1 = display1.getTransformation();
        transformation1.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
        display1.setTransformation(transformation1);

        Transformation transformation2 = display2.getTransformation();
        transformation2.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
        display2.setTransformation(transformation2);
    }

    public void markLaunched() {
        this.launchTime = System.currentTimeMillis();
        this.gravity = DEFAULT_GRAVITY.clone();
    }

    public void remove() {
        if (display1 != null && display1.isValid()) {
            display1.remove();
        }
        if (display2 != null && display2.isValid()) {
            display2.remove();
        }
        expansionPoints.clear();
    }

    @Override
    protected void onTick() {
        // Handle expanding fire ring after impact
        if (this.impacted) {
            handleFireRingExpansion();
            return;
        }

        // Calculate rotation angle based on elapsed time
        final float elapsedTotal = System.currentTimeMillis() - creationTime;
        float angle = (float) ((elapsedTotal / 1000.0) * 360.0 * 0.25); // 0.25 full rotations per second

        // Teleport displays to their orbital positions
        display1.teleport(location);
        display2.teleport(location);

        // Apply rotation around each display's own center (translation stays at 0,0,0)
        Vector3f axis = new Vector3f(1, 1, 0).normalize();

        Transformation transformation1 = display1.getTransformation();
        transformation1.getTranslation().set(0, 0, 0); // Keep at center
        AxisAngle4f rotation1 = new AxisAngle4f((float) Math.toRadians(angle * 2), axis);
        transformation1.getLeftRotation().set(rotation1);
        display1.setTransformation(transformation1);

        Transformation transformation2 = display2.getTransformation();
        transformation2.getTranslation().set(0, 0, 0); // Keep at center
        AxisAngle4f rotation2 = new AxisAngle4f((float) Math.toRadians(angle * 2 + 90), axis);
        transformation2.getLeftRotation().set(rotation2);
        display2.setTransformation(transformation2);

        // Spawn fire particles at center
        if (this.launchTime != 0) {
            final Collection<Player> receivers = location.getNearbyPlayers(60);
            Particle.SMOKE.builder()
                    .count(3)
                    .extra(0)
                    .offset(hitboxSize / 3, hitboxSize / 3, hitboxSize / 3)
                    .location(location)
                    .receivers(receivers)
                    .spawn();
            Particle.TRAIL.builder()
                    .count(3)
                    .extra(0)
                    .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize / 2)
                    .data(new Particle.Trail(location, Color.fromRGB(235, (int) (137 + Math.random() * 100), 0), 15))
                    .location(location)
                    .receivers(receivers)
                    .spawn();
        }

        // Play sound occasionally
        if ((int) (elapsedTotal / 100) % 5 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 0.3F, 1.5F);
        }
    }

    private void handleFireRingExpansion() {
        // Check if we've reached max radius
        final double maxRadius = this.maxRadius.get();
        if (currentRadius >= maxRadius) {
            expansionPoints.clear();
            return;
        }

        // Expand the ring
        currentRadius = Math.min(currentRadius + expansionRate.get(), maxRadius);

        // Create ring of fire particles that hug the ground and walls
        for (Map.Entry<Location, Vector> entry : expansionPoints.entrySet()) {
            final Location point = entry.getKey();
            final Vector direction = entry.getValue();

            // Update the point
            final Location newPoint = findSurfaceLocation(point, direction);
            point.set(newPoint.getX(), newPoint.getY(), newPoint.getZ());

            // Play particles
            spawnFireParticles(point.clone().add(0, 0.1, 0));
            checkPlayerCollision(point);
        }
    }

    private Location findSurfaceLocation(Location point, Vector direction) {
        final double expansionRate = this.expansionRate.get();

        // Check if we're crawling down a wall
        final RayTraceResult groundCheck = point.getWorld().rayTraceBlocks(
                point,
                new Vector(0, -1, 0),
                0.1,
                FluidCollisionMode.NEVER,
                true
        );

        // We need to go down
        if (groundCheck == null) {
            return point.clone().add(0, -expansionRate, 0);
        }

        final RayTraceResult horizontalResult = point.getWorld().rayTraceBlocks(
                point,
                direction,
                expansionRate,
                FluidCollisionMode.NEVER,
                true
        );

        // We didn't hit a wall
        // This means we're either moving horizontally
        // Or crawling down a wall
        if (horizontalResult == null) {
            return point.clone().add(direction.clone().multiply(expansionRate));
        }

        // We hit a wall
        final Vector position = horizontalResult.getHitPosition();
        // Stick away 0.1 from the wall
        return position.toLocation(point.getWorld()).add(0, expansionRate, 0);
    }

    private void spawnFireParticles(Location particleLocation) {
        // Spawn flame particles
        final Collection<Player> receivers = particleLocation.getNearbyPlayers(60);
        final double random = Math.random();
        if (random < 0.6) {
            Particle.FLAME.builder()
                    .count(1)
                    .extra(0)
                    .offset(0.1, 0.1, 0.1)
                    .location(particleLocation)
                    .receivers(receivers)
                    .spawn();
        }
    }

    private void checkPlayerCollision(Location particleLocation) {
        // Check for nearby players
        for (LivingEntity entity : particleLocation.getNearbyLivingEntities(0.3)) {
            if (entity instanceof ArmorStand) continue;
            if (entity.equals(caster)) continue;
            if (!(entity instanceof Player target)) continue;

            // Check if we can hurt this player
            if (!canCollideWith(target)) {
                continue;
            }

            // Apply damage
            final DamageEvent event = UtilDamage.doDamage(new DamageEvent(
                    target,
                    caster,
                    null,
                    new SkillDamageCause(skill).withBukkitCause(EntityDamageEvent.DamageCause.FIRE).withCategory(DamageCauseCategory.RANGED),
                    spreadDamage.get(),
                    "Inferno"
            ));
            if (!event.isCancelled()) {
                // Apply fire ticks
                UtilEntity.setFire(target, caster, fireTicks.get() * 50L);
            }
        }
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity) && entity instanceof LivingEntity living && !UtilEntity.isEntityFriendly(caster, living);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        redirect(new Vector());
        gravity = new Vector();

        Location hitLocation = UtilLocation.getClosestSurfaceBlock(lastLocation, 1.0, true).orElse(lastLocation.clone().subtract(0, 1, 0));
        hitLocation.add(0, 1, 0);

        // Population expanding points
        final double baseAngle = Math.toRadians(Math.random() * 90);
        // Get a random vector going out horizontally at that angle
        final Vector start = new Vector(Math.cos(baseAngle), 0, Math.sin(baseAngle)).multiply(hitboxSize);
        // Get 32 vectors around the center starting at this angle
        final int points = 32;
        for (int i = 1; i <= points; i++) {
            final double angle = Math.toRadians((double) (i * 360) / points);
            final Vector direction = start.clone().rotateAroundY(angle);

            final RayTraceResult rayTraceResult = hitLocation.getWorld().rayTraceBlocks(hitLocation,
                    direction,
                    hitboxSize,
                    FluidCollisionMode.NEVER,
                    true);

            final Location hitPoint;
            if (rayTraceResult != null) {
                hitPoint = rayTraceResult.getHitPosition().toLocation(hitLocation.getWorld());
            } else {
                hitPoint = hitLocation.clone().add(0, 0.05, 0).add(direction);
            }

            expansionPoints.put(hitPoint, direction);
        }

        if (result.getHitEntity() instanceof LivingEntity livingEntity) {
            final DamageEvent event = UtilDamage.doDamage(new DamageEvent(
                    livingEntity,
                    caster,
                    null,
                    new SkillDamageCause(skill).withBukkitCause(EntityDamageEvent.DamageCause.FIRE),
                    impactDamage.get(),
                    "Inferno"
            ));

            if (!event.isCancelled()) {
                new SoundEffect(Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 5.0f).play(caster, caster.getLocation());
            }
        }

        // FX
        display1.remove();
        display2.remove();
        Particle.DUST_PILLAR.builder()
                .location(location)
                .data(Material.REDSTONE_BLOCK.createBlockData())
                .receivers(60)
                .extra(0.8)
                .offset(hitboxSize, hitboxSize, hitboxSize)
                .count(100)
                .spawn();
        new SoundEffect(Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.0f, 0.7f).play(location);
        new SoundEffect(Sound.BLOCK_VAULT_BREAK, 0.0f, 1.3f).play(location);
        new SoundEffect(Sound.BLOCK_CONDUIT_ATTACK_TARGET, 2.0f, 1.3f).play(location);
    }
}
