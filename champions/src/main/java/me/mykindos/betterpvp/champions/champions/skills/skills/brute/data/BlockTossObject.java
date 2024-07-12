package me.mykindos.betterpvp.champions.champions.skills.skills.brute.data;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BlockTossObject {

    @Getter
    private long castTime;
    private final List<BlockData> blockPool;
    private final Skill skill;
    private final Player caster;
    @Getter
    private double size;
    private double damage;
    private double radius;
    @Getter
    private boolean thrown;
    @Getter
    private boolean impacted;
    @Getter
    private int thrownTicks = 0;
    @Getter
    private int impactTicks = 0;

    @Getter
    private Arrow referenceEntity;
    private final List<ArmorStand> vehicles;
    private List<Display> displayBlocks;

    public BlockTossObject(List<BlockData> blockPool, Skill skill, Player caster) {
        this.vehicles = new ArrayList<>();
        this.blockPool = blockPool;
        this.skill = skill;
        this.caster = caster;
    }

    public Location getCenterLocation() {
        return referenceEntity.getLocation().add(0.0, referenceEntity.getHeight() / 2, 0.0);
    }

    public void setSize(double size) {
        this.size = size;
        for (Display display : this.displayBlocks) {
            final Transformation transformation = display.getTransformation();
            display.setTransformation(new Transformation(
                    transformation.getTranslation(),
                    transformation.getLeftRotation(),
                    new Vector3f((float) size),
                    transformation.getRightRotation()));
        }
    }

    public void spawn(double size) {
        this.size = size;
        final Location location = getCastLocation();

        // Create block displays
        displayBlocks = new ArrayList<>();
        final World world = location.getWorld();
        for (int i = 0; i < 10; i++) { // 10 block displays
            BlockDisplay blockDisplay = world.spawn(location, BlockDisplay.class);
            blockDisplay.setInterpolationDelay(0);
            blockDisplay.setTeleportDuration(1);
            blockDisplay.setInterpolationDuration(10);
            final Transformation curTransformation = blockDisplay.getTransformation();

            // Get a random translation so the boulder looks more natural
            // Center the block display by subtracting 0.5, acts as our reference point for offsets
            float xTranslation = (float) ((Math.random()) - 0.5f - 0.5f);
            float yTranslation = (float) ((Math.random()) - 0.5f) - 0.5f;
            float zTranslation = (float) ((Math.random()) - 0.5f - 0.5f);

            Vector3f translation = new Vector3f(xTranslation, yTranslation, zTranslation);
            final Transformation transformation = new Transformation(
                    translation,
                    curTransformation.getLeftRotation(),
                    new Vector3f((float) size),
                    curTransformation.getRightRotation());

            // Set block display data, aka transformation and a random block data from our pool
            blockDisplay.setBlock(getRandomBlock());
            blockDisplay.setTransformation(transformation);
            blockDisplay.setPersistent(false);
            blockDisplay.setBillboard(Display.Billboard.FIXED);

            // Hacky way to make the block display have the same rotation as the player
            displayBlocks.add(blockDisplay);
        }
    }

    private BlockData getRandomBlock() {
        return blockPool.get(UtilMath.randomInt(blockPool.size()));
    }

    public void throwBoulder(double speed, double radius, double damage) {
        Preconditions.checkState(!thrown, "Boulder has already been thrown");
        this.radius = radius;
        this.damage = damage;
        this.castTime = System.currentTimeMillis();
        this.thrown = true;

        // Create reference armor stand
        // The block displays will follow this
        final Location location = caster.getEyeLocation().add(caster.getLocation().getDirection().multiply(1.5));
        referenceEntity = caster.getWorld().spawn(location, Arrow.class, arrow -> {
            arrow.setHasBeenShot(false);
            arrow.setSilent(true);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setPortalCooldown(Integer.MAX_VALUE);
            arrow.setInvulnerable(true);
            arrow.setVisualFire(false);
            arrow.setPersistent(false);
            arrow.setVisibleByDefault(false);
            arrow.setShooter(caster);
        });

        // Throw sound cue
        final SoundEffect sound = new SoundEffect(Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.4f, 0.2f + 0.3f * (float) speed);
        location.getWorld().playSound(sound.getSound(), displayBlocks.get(0));

        // Set the rotation of the block displays to match the player
        for (Display display : displayBlocks) {
            display.setRotation(caster.getYaw(), caster.getPitch());
        }

        // Apply velocity to our reference entity
        referenceEntity.setVelocity(caster.getLocation().getDirection()
                .multiply(new Vector(1.2, 1.4, 1.2))
                .multiply(speed));
    }

    public void tick() {
        if (!thrown) {
            moveToCaster();
            return;
        }

        thrownTicks++;
        referenceEntity.setFallDistance(0f);
        if (!impacted) {
            playCues();
            return;
        }

        impactTicks++;
        playImpactRing();
        playImpactBlocks();
    }

    private void moveToCaster() {
        final Location location = getCastLocation();
        location.setPitch(0);
        location.setYaw(caster.getYaw());
        for (Display display : displayBlocks) {
            display.teleport(location);
        }
    }

    private Location getCastLocation() {
        final float yaw = caster.getYaw() + 90;
        final Vector direction = new Vector(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw)));
        final Location location = caster.getLocation().add(direction);
        final Optional<Location> opt = UtilLocation.getClosestSurfaceBlock(location, 1.0, true);
        if (opt.isPresent()) {
            final Location result = opt.get();
            return result.add(0.0, 1.0, 0.0);
        }
        return location;
    }

    private void playImpactRing() {
        if (impactTicks >= 5) {
            return; // Expand for 0.25 seconds
        }

        // Particle ring
        double particleRadius = impactTicks * radius / 5;
        for (int degree = 0; degree <= 360; degree += 15) {
            final Location absRingPoint = UtilLocation.fromFixedAngleDistance(getCenterLocation(), particleRadius, degree);
            final Optional<Location> ringPoint = UtilLocation.getClosestSurfaceBlock(absRingPoint, 3.0, true);
            if (ringPoint.isEmpty()) {
                continue;
            }

            final Location location = ringPoint.get();
            location.add(0.0, 1.1, 0.0);
            Particle.DUST.builder().color(255, 154, 46).location(location).receivers(60, true).spawn();
        }
    }

    private void playImpactBlocks() {
        if (impactTicks >= 40) {
            return; // Expire after 2 seconds
        }

        // Make the pebbles smaller and rotate them
        for (Display display : displayBlocks) {
            // Scale down, move and rotate
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(4);

            final Transformation transformation = display.getTransformation();
            transformation.getScale().mul(0.96f);

            // rotations
            final Quaternionf leftRotation = transformation.getLeftRotation();
            leftRotation.rotateAxis((float) Math.toRadians(3), getRandomNegative(), 1, getRandomNegative());
            display.setTransformation(transformation);
        }
    }

    private void playCues() {
        // duplicate reference entity velocity
        final Vector velocity = referenceEntity.getVelocity();
        float pitch = (float) (Math.atan2(Math.pow(velocity.getZ(), 2) + Math.pow(velocity.getX(), 2), velocity.getY()) * 180f / Math.PI - 90.0f);

        // Change display rotation SLOWLY
        for (Display displayEnt : displayBlocks) {
            final float oldPitch = displayEnt.getLocation().getPitch();
            final float pitchDifference = pitch - oldPitch;
            final float pitchDelta = Math.max(-10, Math.min(10, pitchDifference));

            final Location location = referenceEntity.getLocation();
            location.setPitch(oldPitch + pitchDelta);
            location.setYaw(displayEnt.getYaw());
            displayEnt.teleport(location);
        }

        // Cues
        final Location location = getCenterLocation();
        final BlockData randomBlock = getRandomBlock();
        Particle.FALLING_DUST.builder().data(randomBlock).location(location).receivers(60, true).spawn();
        Particle.CLOUD.builder().extra(0).location(location).receivers(60, true).spawn();
    }

    // can be run out of main thread
    public void impact(Player caster) {
        if (impacted) {
            return;
        }

        this.impacted = true;
        final Location impactLocation = getCenterLocation();

        // Deconstruction of the boulder
        for (Display display : displayBlocks) {
            // get a random direction to throw this pebble to
            final Vector direction = new Vector(Math.random() * getRandomNegative(), 0, Math.random() * getRandomNegative()).normalize();

            // Assign a new armorstand to move
            final ArmorStand vehicle = UtilEntity.createUtilityArmorStand(impactLocation);
            vehicle.addPassenger(display);
            vehicles.add(vehicle);

            // reset transformations
            final float height = (float) vehicle.getHeight();
            display.setTransformation(new Transformation(new Vector3f(0, -height, 0), new Quaternionf(), new Vector3f((float) size), new Quaternionf()));
            display.setRotation(0f, 0f);

            // Shoot them out
            VelocityData velocityData = new VelocityData(direction, 0.7, false, 0.0, 0.0, 0.0, true);
            UtilVelocity.velocity(vehicle, caster, velocityData);
        }

        // Damage and heal
        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(caster, impactLocation, radius, EntityProperty.ALL);
        if (caster.getLocation().distanceSquared(impactLocation) <= radius * radius) {
            nearby.add(new KeyValue<>(caster, EntityProperty.FRIENDLY));
        }

        final List<LivingEntity> damaged = new ArrayList<>();
        for (KeyValue<LivingEntity, EntityProperty> nearbyEntry : nearby) {
            final LivingEntity ent = nearbyEntry.getKey();
            final EntityProperty relation = nearbyEntry.getValue();

            if (relation != EntityProperty.FRIENDLY) {
                if (!ent.hasLineOfSight(impactLocation)) continue;
                // Damage anybody who is not friendly
                damaged.add(ent);
                Vector knockback = ent.getLocation().toVector().subtract(impactLocation.toVector());
                final double strength = (radius * radius - ent.getLocation().distanceSquared(impactLocation)) / (radius * radius);
                VelocityData velocityData = new VelocityData(knockback, strength, false, 0.0, 0.0, 3.0, true);
                UtilVelocity.velocity(ent, caster, velocityData);
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, caster, null, EntityDamageEvent.DamageCause.CUSTOM, damage, false, skill.getName()));
                UtilMessage.simpleMessage(ent, skill.getName(), "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), skill.getName());
            }
        }

        if (!damaged.isEmpty()) {
            final String nameList = damaged.stream().map(player -> "<alt2>" + player.getName() + "</alt2>").collect(Collectors.joining(", "));
            UtilMessage.simpleMessage(caster, skill.getName(), "You hit %s with <alt>%s</alt>.", nameList, skill.getName());
        }

        impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 1.5f);
    }

    private int getRandomNegative() {
        return Math.random() > 0.5 ? 1 : -1;
    }

    public void despawn() {
        // delay this for a cool effect
        for (Display display : displayBlocks) {
            display.remove();
        }
        for (ArmorStand vehicle : vehicles) {
            vehicle.remove();
        }
        if (referenceEntity.isValid()) {
            referenceEntity.remove();
        }
    }
}