package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.data;

import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.framework.customtypes.CustomArmourStand;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.*;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class BoulderObject {

    private static final Random RANDOM = new Random();
    private final long castTime = System.currentTimeMillis(); // millis
    private final Champions champions;
    private final double heal;
    private final double damage;
    private final double radius;
    private final double maxRadius;
    private final List<BlockData> blockPool;
    private final Skill skill;

    private ArmorStand referenceEntity;
    private List<Display> displayBlocks;
    private BukkitTask task;

    public BoundingBox getBoundingBox() {
        return BoundingBox.of(getCenterLocation(), 1.2, 0.9, 1.2);
    }

    public Location getCenterLocation() {
        return referenceEntity.getLocation().add(0.0, referenceEntity.getHeight() / 2, 0.0);
    }

    public void spawn(Player caster) {
        // Spawn it 0.5 blocks ahead
        final Location location = caster.getEyeLocation().add(caster.getLocation().getDirection().normalize().multiply(0.5));

        // Throw sound cue
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);

        // Create reference armor stand
        // The block displays will follow this
        CustomArmourStand as = new CustomArmourStand(((CraftWorld) location.getWorld()).getHandle());
        ArmorStand armorStand = (ArmorStand) as.spawn(location);
        armorStand.setSmall(true);
        armorStand.setSilent(true);
        armorStand.setPortalCooldown(Integer.MAX_VALUE);
        armorStand.setInvulnerable(true);
        armorStand.setVisualFire(false);
        armorStand.setPersistent(false);
        armorStand.setCollidable(false);
        referenceEntity = armorStand;

        // Create block displays
        displayBlocks = new ArrayList<>();
        final World world = referenceEntity.getWorld();
        for (int i = 0; i < 10; i++) { // 10 block displays
            BlockDisplay blockDisplay = world.spawn(location, BlockDisplay.class);
            final Transformation curTransformation = blockDisplay.getTransformation();

            // Get a random translation so the boulder looks more natural
            // Center the block display by subtracting 0.5, acts as our reference point for offsets
            float xTranslation = (RANDOM.nextFloat(0.8f - 0.2f)) * (RANDOM.nextBoolean() ? 1 : -1) - 0.5f;
            float zTranslation = (RANDOM.nextFloat(0.8f - 0.2f)) * (RANDOM.nextBoolean() ? 1 : -1) - 0.5f;
            float yTranslation = (RANDOM.nextFloat(0.8f - 0.2f)) * (RANDOM.nextBoolean() ? 1 : -1);
            yTranslation -= referenceEntity.getHeight() / 2f; // offset to be in the middle of the reference entity

            Vector3f translation = new Vector3f(xTranslation, yTranslation, zTranslation);
            final Transformation transformation = new Transformation(
                    translation,
                    curTransformation.getLeftRotation(),
                    curTransformation.getScale(),
                    curTransformation.getRightRotation());

            // Set block display data, aka transformation and a random block data from our pool
            blockDisplay.setBlock(blockPool.get(RANDOM.nextInt(blockPool.size())));
            blockDisplay.setTransformation(transformation);

            // Hacky way to make the block display have the same rotation as the player
            blockDisplay.teleport(caster.getLocation());
            referenceEntity.addPassenger(blockDisplay);
            displayBlocks.add(blockDisplay);
        }

        // Apply velocity to our reference entity
        referenceEntity.setVelocity(location.getDirection().multiply(new Vector(2.0, 1.7, 2.0)));

        startInterpolations();
    }

    private void startInterpolations() {
        task = new BukkitRunnable() {
            @Override
            public void run() {

                // duplicate reference entity velocity
                final Vector velocity = referenceEntity.getVelocity();
                float pitch = (float) (Math.atan2(Math.pow(velocity.getZ(), 2) + Math.pow(velocity.getX(), 2), velocity.getY()) * 180f / Math.PI - 90.0f);

                // Change display rotation SLOWLY
                for (Display displayEnt : displayBlocks) {
                    final float oldPitch = displayEnt.getLocation().getPitch();
                    final float pitchDifference = pitch - oldPitch;
                    final float pitchDelta = Math.max(-10, Math.min(10, pitchDifference));

                    displayEnt.setRotation(displayEnt.getLocation().getYaw(), oldPitch + pitchDelta);
                }

                // Remove armor stand fall particles
                referenceEntity.setFallDistance(0f);

                // Cues
                final Location location = getCenterLocation();
                final BlockData randomBlock = blockPool.get(RANDOM.nextInt(blockPool.size()));
                Particle.FALLING_DUST.builder().data(randomBlock).location(location).receivers(60, true).spawn();
                Particle.CLOUD.builder().extra(0).location(location).receivers(60, true).spawn();
            }
        }.runTaskTimer(champions, 0L, 1L);
    }

    // can be run out of main thread
    public void impact(Player caster) {
        final Location impactLocation = getCenterLocation();

        // Deconstruction of the boulder
        final int tickDelay = 5;
        Map<Display, Vector> directions = new HashMap<>();
        for (Display display : displayBlocks) {
            // reset transformations
            display.setTransformation(new Transformation(new Vector3f(0), new Quaternionf(), new Vector3f(1f), new Quaternionf()));
            display.teleport(referenceEntity.getLocation().add(-0.5, 0.0, -0.5)); // locate them in the center bottom, so they can start rolling
            display.setRotation(0f, 0f);

            // get a random direction to throw this pebble to
            final Vector direction = new Vector(RANDOM.nextFloat() * getRandomNegative(), 0, RANDOM.nextFloat() * getRandomNegative()).normalize();
            referenceEntity.removePassenger(display);
            direction.setY(0);
            directions.put(display, direction);
        }

        final int maxDeconstructTicks = 40;
        new BukkitRunnable() {
            int ticks = 0;
            Vector3f scale = displayBlocks.get(0).getTransformation().getScale();

            @Override
            public void run() {
                ticks += tickDelay;
                if (ticks >= maxDeconstructTicks) {
                    cancel();
                    return;
                }

                scale = scale.mul(0.9f);
                for (Display display : displayBlocks) {
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(tickDelay);

                    // Scale down, move and rotate
                    final Transformation transformation = display.getTransformation();
                    final Vector3f translation = transformation.getTranslation();

                    // Gravity
                    // get location of the translation and check if it's grounded by X amount of blocks
                    // if it is not, then add gravity
                    final Location location = display.getLocation().add(new Vector(translation.x, translation.y, translation.z));
                    final Block under = location.getBlock().getRelative(BlockFace.DOWN);
                    float xGravityFactor = 1;
                    float yGravityDelta = 0;
                    if (UtilBlock.airFoliage(under) || !UtilBlock.solid(under)) {
                        xGravityFactor = 0.2f;
                        yGravityDelta = -2f;
                    }

                    final Vector dir = directions.get(display);
                    Vector3f newTranslation = new Vector3f(
                                    (float) dir.getX() * xGravityFactor * ticks / tickDelay,
                                    -0.07f * ticks / tickDelay + yGravityDelta,
                                    (float) dir.getZ() * xGravityFactor * ticks / tickDelay);

                    // rotations
                    final Quaternionf leftRotation = transformation.getLeftRotation();
                    leftRotation.rotateAxis((float) Math.toRadians(10), getRandomNegative(), 1, getRandomNegative());
                    final Quaternionf rightRotation = transformation.getRightRotation();
                    //                    rightRotation.rotateAxis((float) Math.toRadians(10), 0, 1, 0);

                    display.setTransformation(new Transformation(newTranslation, leftRotation, scale, rightRotation));
                }
            }
        }.runTaskTimer(champions, 0L, tickDelay);

        // Particles
        final int maxRadiusTicks = 5; // Expand to max radius over half a second
        new BukkitRunnable() {
            double radius = 0;
            double ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks >= maxRadiusTicks) {
                    cancel();
                    return;
                }

                // Particle ring
                radius += maxRadius / maxRadiusTicks;
                for (int degree = 0; degree <= 360; degree += 15) {
                    final Location absRingPoint = UtilLocation.fromFixedAngleDistance(impactLocation, radius, degree);
                    final Optional<Location> ringPoint = UtilLocation.getClosestSurfaceBlock(absRingPoint, 3.0, true);
                    if (ringPoint.isEmpty()) {
                        continue;
                    }

                    final Location location = ringPoint.get();
                    location.add(0.0, 1.1, 0.0);
                    Particle.REDSTONE.builder().color(255, 154, 46).location(location).receivers(60, true).spawn();
                }
            }
        }.runTaskTimer(champions, 0L, 2L);

        // Damage and heal
        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(caster, impactLocation, radius, EntityProperty.ALL);
        final List<Player> healed = new ArrayList<>();
        final List<LivingEntity> damaged = new ArrayList<>();
        for (KeyValue<LivingEntity, EntityProperty> nearbyEntry : nearby) {
            final LivingEntity ent = nearbyEntry.getKey();
            final EntityProperty relation = nearbyEntry.getValue();

            if (relation == EntityProperty.FRIENDLY && ent instanceof Player ally) {
                // Heal ally
                healed.add(ally);
                UtilPlayer.health(ally, getHeal());
                Particle.VILLAGER_HAPPY.builder().location(ally.getLocation()).offset(0.0, ally.getHeight() + 0.2, 0.0).receivers(60, true).spawn();
            } else {
                // Damage anybody else
                damaged.add(ent);
                Vector knockback = ent.getLocation().toVector().subtract(impactLocation.toVector());
                final double strength = ent.getLocation().distanceSquared(impactLocation) * 0.5 / radius;
                UtilVelocity.velocity(ent, knockback, strength, false, 0.0, 0.0, 3.0, true);
            }
        }

        if (!healed.isEmpty()) {
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            final String nameList = healed.stream().map(player -> "<alt2>" + player.getName() + "</alt2>").collect(Collectors.joining(", "));
            UtilMessage.simpleMessage(caster, skill.getName(), "You healed %s for <alt>%s</alt> health.", nameList, getHeal());

            healed.forEach(player -> {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                UtilMessage.simpleMessage(player, skill.getName(), "<alt2>%s</alt2> healed you for <alt>%s</alt> health.", caster.getName(), getHeal());
            });
        }

        if (!damaged.isEmpty()) {
            final String nameList = damaged.stream().map(player -> "<alt2>" + player.getName() + "</alt2>").collect(Collectors.joining(", "));
            UtilMessage.simpleMessage(caster, skill.getName(), "You hit %s with <alt>%s</alt>.", nameList, skill.getName());

            healed.forEach(player -> {
                player.getWorld().playSound(impactLocation, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                UtilMessage.simpleMessage(player, skill.getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
            });
        }

        impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
    }

    private int getRandomNegative() {
        return RANDOM.nextBoolean() ? 1 : -1;
    }

    public void despawn() {
        // delay this for a cool effect
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Display display : displayBlocks) {
                    display.remove();
                }
            }
        }.runTaskLater(champions, 25L);
        referenceEntity.remove();
        task.cancel();
    }
}