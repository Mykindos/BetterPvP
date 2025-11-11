package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public class BlizzardProjectile extends Projectile {

    private static final NamespacedKey ATTRIBUTE_KEY = new NamespacedKey("champions", "blizzard_projectile");
    private final List<LivingEntity> trapped = new ArrayList<>();
    private final ItemDisplay display1;
    private final ItemDisplay display2;
    private final Skill skill;
    private final ChampionsManager championsManager;
    private long launchTime;
    private double displaySize;
    private double initialSpeed;

    public BlizzardProjectile(Player caster, Location location, double hitboxSize, double displaySize,
                              long aliveTime, Skill skill, ChampionsManager championsManager) {
        super(caster, hitboxSize, location, aliveTime);
        this.skill = skill;
        this.championsManager = championsManager;
        this.displaySize = displaySize;

        // Create first snow block display
        this.display1 = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(new ItemStack(Material.SNOW_BLOCK));
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });

        // Create second snow block display (opposite side)
        this.display2 = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(new ItemStack(Material.SNOW_BLOCK));
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set((float) displaySize, (float) displaySize, (float) displaySize);
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    @Override
    public boolean isExpired() {
        return this.launchTime != 0 && UtilTime.elapsed(launchTime, aliveTime);
    }

    public void markLaunched() {
        this.launchTime = System.currentTimeMillis();
        this.initialSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        this.gravity = DEFAULT_GRAVITY.clone();
    }

    private void shrink(LivingEntity livingEntity, double targetScale) {
        final AttributeInstance gravityAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.GRAVITY));
        gravityAttribute.removeModifier(ATTRIBUTE_KEY);
        gravityAttribute.addTransientModifier(new AttributeModifier(ATTRIBUTE_KEY, 0, AttributeModifier.Operation.ADD_SCALAR));

        final AttributeInstance scaleAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.SCALE));
        scaleAttribute.removeModifier(ATTRIBUTE_KEY);
        scaleAttribute.addModifier(new AttributeModifier(ATTRIBUTE_KEY, targetScale - scaleAttribute.getBaseValue(), AttributeModifier.Operation.ADD_SCALAR));

        final AttributeInstance speedAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED));
        speedAttribute.removeModifier(ATTRIBUTE_KEY);
        speedAttribute.addModifier(new AttributeModifier(ATTRIBUTE_KEY, 0, AttributeModifier.Operation.ADD_SCALAR));
    }

    private void expand(LivingEntity livingEntity) {
        final AttributeInstance gravityAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.GRAVITY));
        gravityAttribute.removeModifier(ATTRIBUTE_KEY);

        final AttributeInstance scaleAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.SCALE));
        scaleAttribute.removeModifier(ATTRIBUTE_KEY);

        final AttributeInstance speedAttribute = Objects.requireNonNull(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED));
        speedAttribute.removeModifier(ATTRIBUTE_KEY);
    }

    public void remove() {
        if (display2 != null && display2.isValid()) {
            display2.remove();
        }
        if (display1 != null && display1.isValid()) {
            display1.remove();
        }

        // Untrap the living entities
        for (LivingEntity entity : trapped) {
            expand(entity);
            entity.setVelocity(new Vector());
        }
        trapped.clear();
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        // Trap the entity if one is present
        if (launchTime != 0) {
            if (result.getHitEntity() instanceof LivingEntity livingEntity) {
                if (trapped.contains(livingEntity)) {
                    return CollisionResult.CONTINUE;
                }

                shrink(livingEntity, 0.1);
                trapped.add(livingEntity);
            }

            // FX
            Particle.SNOWFLAKE.builder()
                    .location(location)
                    .receivers(60)
                    .extra(0.1)
                    .offset(hitboxSize, hitboxSize, hitboxSize)
                    .count(30)
                    .spawn();
            Particle.BLOCK.builder()
                    .location(location)
                    .data(Material.SNOW_BLOCK.createBlockData())
                    .receivers(60)
                    .extra(0.5)
                    .offset(hitboxSize, hitboxSize, hitboxSize)
                    .count(20)
                    .spawn();
            location.getWorld().playSound(location, Sound.BLOCK_SNOW_BREAK, 1.5F, 0.8F);
        }

        return CollisionResult.REFLECT_BLOCKS;
    }

    @Override
    protected void onTick() {
        for (LivingEntity entity : trapped) {
            final Vector direction = location.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1);
            entity.setVelocity(direction);
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

        // Spawn snow particles at center
        if (this.launchTime != 0) {
            final Collection<Player> receivers = location.getNearbyPlayers(60);
            Particle.SNOWFLAKE.builder()
                    .count(5)
                    .extra(0)
                    .offset(hitboxSize / 3, hitboxSize / 3, hitboxSize / 3)
                    .location(location)
                    .receivers(receivers)
                    .spawn();
            Particle.DUST.builder()
                    .count(3)
                    .extra(0)
                    .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize / 2)
                    .data(new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f))
                    .location(location)
                    .receivers(receivers)
                    .spawn();
        }

        // Play sound occasionally
        if ((int) (elapsedTotal / 100) % 5 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_SNOW_STEP, 0.3F, 1.0F);
        }
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity) && entity instanceof LivingEntity living && !UtilEntity.isEntityFriendly(caster, living);
    }
}
