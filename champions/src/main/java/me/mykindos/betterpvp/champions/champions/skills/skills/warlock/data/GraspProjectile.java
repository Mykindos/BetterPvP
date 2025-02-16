package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GraspProjectile extends Projectile {

    private final List<Display> displays = new ArrayList<>();
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final Consumer<LivingEntity> impactCallback;

    public GraspProjectile(Player caster, double size, Location location, long aliveTime, Material material, Consumer<LivingEntity> impactCallback) {
        super(caster, size, location, aliveTime);
        this.impactCallback = impactCallback;

        for (int i = 0; i < 20; i++) {
            final Vector3f translation = new Vector3f((float) UtilMath.randDouble(-size, size),
                    (float) UtilMath.randDouble(-size, size),
                    (float) UtilMath.randDouble(-size, size));

            Display display = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setItemStack(new ItemStack(material));
                spawned.setTransformation(new Transformation(
                        translation,
                        new AxisAngle4f(),
                        new Vector3f(1.2f),
                        new AxisAngle4f()
                ));
            });
            display.teleport(display.getLocation().setDirection(caster.getLocation().getDirection()));
            display.setInterpolationDelay(1);
            display.setTeleportDuration(1);
            displays.add(display);
        }
    }

    public void remove() {
        displays.forEach(display -> {
            if (display != null && display.isValid()) {
                display.remove();
            }
        });
        displays.clear();
    }

    @Override
    protected void onTick() {
        for (Display display : displays) {
            display.teleport(this.location.clone().setDirection(display.getLocation().getDirection().add(new Vector(
                    UtilMath.randDouble(-0.2, 0.2),
                    UtilMath.randDouble(-0.2, 0.2),
                    UtilMath.randDouble(-0.2, 0.2)
            ))));
        }

        if (Bukkit.getCurrentTick() % 2 == 0) {
            location.getWorld().playSound(location, Sound.ENTITY_VEX_DEATH, 0.3f, 0.3f);
        }
        Particle.ASH.builder()
                .count(100)
                .offset(hitboxSize / 2, hitboxSize / 2, hitboxSize / 2)
                .location(location)
                .receivers(60)
                .spawn();
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        if (!super.canCollideWith(entity) || hitEntities.contains(entity)) {
            return false;
        }

        final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, (LivingEntity) entity);
        event.callEvent();
        return event.getResult() != Event.Result.DENY;
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        if (!(result.getHitEntity() instanceof LivingEntity target) || hitEntities.contains(target)) {
            return CollisionResult.CONTINUE;
        }
        hitEntities.add(target);
        impactCallback.accept(target);
        return CollisionResult.CONTINUE;
    }
}
