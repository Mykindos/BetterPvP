package me.mykindos.betterpvp.core.effects.types.negative;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.projectile.VineProjectile;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Causes vines to pull down the player and slow them down (attribute modifier)
 */
public class EntangledEffect extends EffectType {

    private static final NamespacedKey NAMESPACED_KEY = new NamespacedKey("betterpvp", "entangled");
    private final Multimap<Effect, VineDecoration> decorations = HashMultimap.create();
    private final Map<Effect, VineProjectile> vines = new HashMap<>();

    private double getSpeedModifier(int amplifier) {
        return Math.pow(0.75, Math.max(1, amplifier + 1));
    }

    private AttributeModifier getModifier(int amplifier) {
        return new AttributeModifier(NAMESPACED_KEY, -(1 - getSpeedModifier(amplifier)), AttributeModifier.Operation.ADD_SCALAR);
    }

    private AttributeModifier getJumpModifier(int amplifier) {
        return new AttributeModifier(NAMESPACED_KEY, -Integer.MAX_VALUE, AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        // Get immediate highest location below location
        final Optional<Location> closestSurfaceBelow = UtilLocation.getClosestSurfaceBelow(livingEntity.getLocation());
        if (closestSurfaceBelow.isEmpty()) {
            return;
        }

        // Slow the player down
        final Location targetLocation = closestSurfaceBelow.get();
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED)).removeModifier(NAMESPACED_KEY);
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.JUMP_STRENGTH)).removeModifier(NAMESPACED_KEY);
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED)).addTransientModifier(getModifier(effect.getAmplifier()));
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.JUMP_STRENGTH)).addTransientModifier(getJumpModifier(effect.getAmplifier()));

        // Only pull them down if theyre in the air
        final Location vineLocation = UtilLocation.getClosestSurfaceBlock(targetLocation, 3.0, true)
                .orElse(targetLocation.clone());
        final Vector distance = livingEntity.getLocation().clone().subtract(vineLocation).toVector();
        // we want it to reach in half a second
        final double length = distance.length();
        final double speed = Math.min(250, length * 4);
        final VineProjectile projectile = new VineProjectile(null,
                0.6,
                vineLocation.add(0, 1.1, 0).setDirection(new Vector()),
                effect.getRawLength(),
                0, // We don't want pull time to extend over alive time
                speed / 7,
                effect.getAmplifier(),
                effect.getRawLength(),
                "Entangled Vines");
        final Vector direction = distance.normalize();
        projectile.redirect(direction.multiply(speed));
        this.vines.put(effect, projectile);

        // Spawn 10 decorations around
        for (int i = 0; i < 50; i++) {
            final Vector offset = new Vector(Math.random() * 7 - 3.5, 0,Math.random() * 7 - 3.5);
            final Location offsetLoc = targetLocation.clone().add(offset);
            final Optional<Location> closestSurfaceBlock = UtilLocation.getClosestSurfaceBlock(offsetLoc, 3.0, true);
            if (closestSurfaceBlock.isEmpty()) continue;

            final Location result = closestSurfaceBlock.get().clone().add(0, 1.1, 0);
            this.decorations.put(effect, new VineDecoration(result));
        }
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        if (!livingEntity.isValid()) {
            final VineProjectile existing = this.vines.remove(effect);
            if (existing != null) {
                existing.remove();
            }

            final Collection<VineDecoration> decorations = this.decorations.get(effect);
            for (VineDecoration decoration : decorations) {
                decoration.remove();
            }
            return;
        }

        // Tick projectiles
        if (this.vines.containsKey(effect)) {
            this.vines.get(effect).tick();
            final VineProjectile vine = this.vines.get(effect);
            if (vine.isMarkForRemoval() || vine.hasFinishedPulling() || vine.isExpired()) {
                vine.remove();
                this.vines.remove(effect);
            }
        }

        // Tick decorations
        final Collection<VineDecoration> decorations = this.decorations.get(effect);
        decorations.forEach(VineDecoration::tick);
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        // Remove attribute modifier
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.MOVEMENT_SPEED)).removeModifier(NAMESPACED_KEY);
        Objects.requireNonNull(livingEntity.getAttribute(Attribute.JUMP_STRENGTH)).removeModifier(NAMESPACED_KEY);

        final VineProjectile vine = this.vines.remove(effect);
        if (vine != null) {
            vine.remove();
        }

        // Remove decorations
        final Collection<VineDecoration> decorations = this.decorations.get(effect);
        decorations.forEach(VineDecoration::remove);
    }

    @Override
    public String getName() {
        return "Entangled";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> players will be reached by vines and pulled down to the ground, " +
                "slowing them down by <stat>" + UtilFormat.formatNumber((1 - getSpeedModifier(level)) * 100, 2) + "%</stat>.";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white> players will be reached by vines and pulled down to the ground, slowing them down.";
    }

    private static class VineDecoration {

        private final BlockDisplay blockDisplay;
        private final Vector3f translation = new Vector3f();
        private final float speed; // blocks per tick

        private VineDecoration(Location spawnLocation) {
            this.speed = 0.1f;
            final List<Material> pool = new ArrayList<>(List.of(Material.TALL_GRASS,
                    Material.LARGE_FERN,
                    Material.WITHER_ROSE,
                    Material.LILY_OF_THE_VALLEY,
                    Material.ROSE_BUSH,
                    Material.BUSH,
                    Material.CAVE_VINES_PLANT,
                    Material.CAVE_VINES,
                    Material.SHORT_GRASS));
            Collections.shuffle(pool);
            this.blockDisplay = spawn(spawnLocation.clone().setDirection(new Vector()), pool.getFirst().createBlockData());
            this.translation.y = (float) -1.5f;
        }

        private BlockDisplay spawn(Location location, BlockData blockData) {
            final AxisAngle4f rotation = new AxisAngle4f();
            final Vector3f scale = new Vector3f((float) (Math.random() * 0.5 + 0.5f));
            final Vector3f translation = new Vector3f(0, -100, 0); // start far below ground

            location.setDirection(new Vector(Math.random(), Math.random(), Math.random()).normalize());
            return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setBrightness(new Display.Brightness(15, 15));
                spawned.setTransformation(new Transformation(translation, rotation, scale, new AxisAngle4f()));
                spawned.setBlock(blockData);
            });
        }

        protected void tick() {
            if (Math.random() < 0.05) {
                final Location location = this.blockDisplay.getLocation();
                new SoundEffect(Sound.BLOCK_GRASS_BREAK, (float) Math.random(), 1f).play(location);
                Particle.BLOCK.builder()
                        .location(location)
                        .data(this.blockDisplay.getBlock())
                        .offset(0.5, 0.5, 0.5)
                        .count(40)
                        .allPlayers()
                        .extra(0)
                        .spawn();

                Particle.HAPPY_VILLAGER.builder()
                        .location(location.clone().add(0, 1, 0))
                        .offset(1, 1, 1)
                        .count(1)
                        .allPlayers()
                        .extra(0)
                        .spawn();
            }

            if (this.translation.y >= -0.5) {
                return; // Let it linger at the top
            }

            this.translation.y += this.speed;
            final Transformation oldTransformation = this.blockDisplay.getTransformation();
            this.blockDisplay.setTransformation(new Transformation(
                    translation,
                    oldTransformation.getLeftRotation(),
                    oldTransformation.getScale(),
                    oldTransformation.getRightRotation()
            ));
        }

        protected void remove() {
            if (this.blockDisplay.isValid()) {
                this.blockDisplay.remove();
            }
        }

    }

}
