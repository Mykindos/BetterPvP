package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class LightningOrbProjectile extends Projectile {

    private final double attachRadius;
    private final List<ItemDisplay> displays = new ArrayList<>();
    private final long impactDelay;
    private long lastAttack;
    private final Map<LivingEntity, Long> entitiesHit = new HashMap<>();
    private final Consumer<LivingEntity> onAttach;

    public LightningOrbProjectile(Player caster, double hitboxSize, Location location, long aliveTime, double attachRadius, Consumer<LivingEntity> onAttach) {
        super(caster, hitboxSize, location, aliveTime);
        this.attachRadius = attachRadius;
        this.onAttach = onAttach;
        this.gravity = DEFAULT_GRAVITY.clone();
        this.dragCoefficient = DEFAULT_DRAG_COEFFICIENT;
        this.impactDelay = 500L;

        for (double i = 0; i < 180; i += 10) {
            displays.add(createDisplay(location, i, hitboxSize * 2));
        }
    }

    @Override
    public boolean isExpired() {
        return impacted ? UtilTime.elapsed(impactTime, aliveTime) : super.isExpired();
    }

    private ItemDisplay createDisplay(Location location, double rotation, double size) {
        location = location.clone();
        final Vector direction = new Vector(0, 0, 1);
        direction.rotateAroundY(Math.toRadians(rotation));
        location.setDirection(direction);

        return location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(new ItemStack(Material.ENDER_PEARL));
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set((float) size, (float) size, (float) size);
            spawned.setTransformation(transformation);
            spawned.setInterpolationDuration(1);
            spawned.setTeleportDuration(1);
        });
    }

    private List<VectorLine> getBranchedLines(Location origin, int branches, double length, double step) {
        List<VectorLine> lines = new ArrayList<>();
        for (int i = 0; i < branches; i++) {
            Vector direction = new Vector(
                    (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 2
            ).normalize().multiply(length);

            Location last = origin.clone();
            for (int j = 0; j < 6; j++) {
                Vector offset = direction.clone()
                        .multiply((j + 1.0) / branches)
                        .add(new Vector(
                                (Math.random() - 0.5) * 0.4,
                                (Math.random() - 0.5) * 0.4,
                                (Math.random() - 0.5) * 0.4
                        ));

                Location next = origin.clone().add(offset);
                lines.add(VectorLine.withStepSize(last, next, step));
                last = next;
            }
        }
        return lines;
    }

    private Location getValidOrigin() {
        return entitiesHit.entrySet().stream()
                .filter(e -> {
                    LivingEntity entity = e.getKey();
                    return entity != null && entity.isValid() && !entity.isDead();
                })
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(e -> e.getKey().getEyeLocation())
                .orElse(location.clone());
    }


    private void spawnElectricity(Location origin) {
        List<VectorLine> branches = getBranchedLines(origin, 2, attachRadius / 3, 0.2);
        for (VectorLine line : branches) {
            boolean spark = Math.random() < 0.5;
            for (Location point : line.toLocations()) {
                if (spark) {
                    Particle.ELECTRIC_SPARK.builder()
                            .location(point)
                            .receivers(60)
                            .extra(0.1)
                            .offset(0.1, 0.1, 0.1)
                            .count(5)
                            .spawn();
                }

                Particle.TRAIL.builder()
                        .location(point)
                        .data(new Particle.Trail(point, Color.fromRGB(255, 255, 255), 15))
                        .receivers(60)
                        .extra(0)
                        .offset(0, 0, 0)
                        .count(1)
                        .spawn();
            }
        }
        new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 0.6f, 1.3f).play(origin);
        new SoundEffect(Sound.ENTITY_BEE_HURT, 0f, 1.3f).play(origin);
    }

    @Override
    protected void onTick() {
        for (ItemDisplay display : displays) {
            display.teleport(location.clone().setDirection(display.getLocation().getDirection()));
        }

        if (isImpacted()) {
            Location origin = getValidOrigin();
            if (Math.random() < 0.1) {
                spawnElectricity(location);
            }

            if (!UtilTime.elapsed(impactTime, impactDelay)) {
                redirect(new Vector(0, 1.2, 0));
                gravity = new Vector();
                dragCoefficient = 0.0;

                for (ItemDisplay display : displays) {
                    final Transformation transformation = display.getTransformation();
                    transformation.getScale().add(new Vector3f(0.1f));
                    display.setTransformation(transformation);
                }
                return;
            }

            if (UtilTime.elapsed(lastAttack, 100L)) {
                Optional<LivingEntity> nearest = UtilEntity.getNearbyEnemies(caster, origin, attachRadius)
                        .stream()
                        .filter(entity -> !entitiesHit.containsKey(entity))
                        .filter(this::canCollideWith)
                        .min(Comparator.comparingDouble(a -> a.getLocation().distanceSquared(origin)))
                        .or(() -> UtilEntity.getNearbyEnemies(caster, location, attachRadius)
                                .stream()
                                .filter(entity -> UtilTime.elapsed(entitiesHit.getOrDefault(entity, 0L), 1000L))
                                .filter(this::canCollideWith)
                                .min(Comparator.comparingDouble(a -> a.getLocation().distanceSquared(origin))));

                if (nearest.isPresent()) {
                    LivingEntity target = nearest.get();
                    entitiesHit.put(target, System.currentTimeMillis());
                    onAttach.accept(target);
                    new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 2.3f).play(origin);
                    new SoundEffect(Sound.ENTITY_BEE_HURT, 0f, 1.3f).play(origin);

                    for (Location particlePoint : VectorLine.withStepSize(origin, target.getEyeLocation(), 0.5).toLocations()) {
                        Particle.END_ROD.builder()
                                .location(particlePoint)
                                .receivers(60)
                                .extra(0)
                                .offset(0, 0, 0)
                                .count(1)
                                .spawn();
                    }

                    lastAttack = System.currentTimeMillis();
                }
            } else if (!entitiesHit.isEmpty()) {
                Particle.ELECTRIC_SPARK.builder()
                        .location(origin)
                        .receivers(60)
                        .extra(0.1)
                        .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize / 2)
                        .count(5)
                        .spawn();
            }

            return;
        }

        // In-flight visual
        Particle.DUST.builder()
                .color(Color.TEAL)
                .location(location)
                .receivers(60)
                .extra(0.1)
                .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize / 2)
                .count(5)
                .spawn();
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity) && entity instanceof LivingEntity;
    }

    public void remove() {
        for (ItemDisplay display : displays) {
            if (display.isValid()) display.remove();
        }

        Particle.POOF.builder()
                .location(location)
                .receivers(30)
                .extra(0.5)
                .offset(0, 0, 0)
                .count(30)
                .spawn();

        new SoundEffect(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2.5f, 2.5f).play(location);
        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 2.5f).play(location);
    }
}
