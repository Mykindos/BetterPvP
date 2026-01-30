package me.mykindos.betterpvp.champions.item.projectile;

import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VineSnareProjectile extends Projectile {

    private final List<Display> displays;
    private final String name;
    private final EffectManager effectManager;
    private final int entangleAmplifier;
    private final long entangleDuration;

    public VineSnareProjectile(@Nullable Player caster, double hitboxSize, Location location, long aliveTime, String name, EffectManager effectManager, int entangleAmplifier, long entangleDuration) {
        super(caster, hitboxSize, location, aliveTime);
        this.name = name;
        this.effectManager = effectManager;
        this.entangleAmplifier = entangleAmplifier;
        this.entangleDuration = entangleDuration;
        this.displays = new ArrayList<>();
        this.displays.add(createMainDisplay());
        for (int i = 0; i < 10; i++) {
            this.displays.add(createSecondaryDisplay());
        }
        this.gravity = DEFAULT_GRAVITY;
        this.dragCoefficient = DEFAULT_DRAG_COEFFICIENT * 3;
    }

    private Display createMainDisplay() {
        return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setBlock(Material.KELP_PLANT.createBlockData());
            final Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-hitboxSize);
            transformation.getScale().set(hitboxSize * 2);
            spawned.setTransformation(transformation);
            spawned.teleport(location.clone().setDirection(new Vector()));
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    private Display createSecondaryDisplay() {
        return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setBlock(Material.KELP_PLANT.createBlockData());
            final Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-hitboxSize / 2);
            transformation.getTranslation().add(
                    (float) ((Math.random() * 2 - 1) * hitboxSize),
                    (float) ((Math.random() * 2 - 1) * hitboxSize),
                    (float) ((Math.random() * 2 - 1) * hitboxSize)
            );
            transformation.getScale().set(hitboxSize * 2 / 2);
            spawned.setTransformation(transformation);
            spawned.teleport(location.clone().setDirection(new Vector(Math.random(), Math.random(), Math.random())));
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    @Override
    protected void onTick() {
        final Vector axis = getVelocity().clone().normalize(); // direction of flight
        final double angle = Math.toRadians(3); // spin step per tick

        for (Display display : displays) {
            Vector current = display.getLocation().getDirection().normalize();
            Vector next = current.clone().rotateAroundAxis(axis, angle).normalize();
            final Location location = this.location.clone();
            if (next.toVector3d().isFinite()) {
                location.setDirection(next);
            }
            display.teleport(location);
        }

        // Play cool particles
        Particle.HAPPY_VILLAGER.builder()
                .location(location)
                .count(4)
                .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize /2)
                .extra(0)
                .allPlayers()
                .spawn();
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        markForRemoval = true;

        Particle.FLASH.builder()
                .color(Color.WHITE)
                .location(location)
                .count(5)
                .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize /2)
                .extra(0)
                .allPlayers()
                .spawn();

        Particle.ITEM_SLIME.builder()
                .location(location)
                .count(300)
                .offset(hitboxSize, hitboxSize, hitboxSize)
                .extra(0)
                .allPlayers()
                .spawn();

        new SoundEffect(Sound.BLOCK_WET_GRASS_BREAK, 0F, 3F).play(location);
        new SoundEffect(Sound.BLOCK_CAVE_VINES_PICK_BERRIES, 0.3F, 3F).play(location);
        new SoundEffect(Sound.BLOCK_HEAVY_CORE_BREAK, 0.3F, 3F).play(location);
        new SoundEffect(Sound.BLOCK_CHERRY_WOOD_TRAPDOOR_CLOSE, 0.3F, 3F).play(location);

        final Entity entity = result.getHitEntity();
        if (entity instanceof ArmorStand || !(entity instanceof LivingEntity target)) {
            return;
        }

        entity.setVelocity(new Vector());
        effectManager.addEffect(target, caster, EffectTypes.ENTANGLED, name, entangleAmplifier, entangleDuration, false);
        if (caster != null) {
            UtilMessage.simpleMessage(caster, name, "You hit <yellow>%s</yellow> with <alt>%s</alt>.", target.getName(), name);
        }
        UtilMessage.simpleMessage(target, name, "<alt2>%s</alt2> hit you with <alt>%s</alt>.", caster.getName(), name);
    }

    public void remove() {
        displays.forEach(Display::remove);
    }
}
