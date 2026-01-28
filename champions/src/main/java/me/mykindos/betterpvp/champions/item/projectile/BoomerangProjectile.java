package me.mykindos.betterpvp.champions.item.projectile;

import lombok.Getter;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbilityDamageCause;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE;

@Getter
public class BoomerangProjectile extends Projectile {

    private boolean recalled = false;
    private final String name;
    private final double damage;
    private final double impactVelocity;
    private final ItemDisplay itemDisplay;
    private final ItemAbility ability;
    private long recallTime = 0;

    public BoomerangProjectile(
            String name,
            @NotNull Player caster,
            double hitboxSize,
            Location location,
            long aliveTime,
            double damage,
            double impactVelocity,
            ItemInstance hammer,
            ItemAbility ability) {
        super(caster, hitboxSize, location, aliveTime);
        this.name = name;
        this.damage = damage;
        this.impactVelocity = impactVelocity;
        this.ability = ability;

        this.itemDisplay = location.getWorld().spawn(location, ItemDisplay.class, spawned -> {
            spawned.setItemStack(hammer.createItemStack());
            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);

            // Model is already oriented correctly with BlockBench
            // So we use HEAD (this also works for throwing it with a Trident)
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
        });
    }

    @Override
    public boolean isExpired() {
        return recalled ? UtilTime.elapsed(recallTime, aliveTime) : super.isExpired();
    }

    public void recall() {
        if (recalled) {
            return; // Already recalled
        }
        recalled = true;
        recallTime = System.currentTimeMillis();
    }

    @Override
    protected void onTick() {
        // Caster is guaranteed to be non-null here because whoever calls this method
        // is responsible for ensuring the caster is not null.
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        for (Location point : interpolateLine()) {
            Particle.END_ROD.builder()
                    .count(1)
                    .extra(0.2)
                    .offset(0.1, 0.1, 0.1)
                    .location(point)
                    .receivers(receivers)
                    .spawn();
        }


        // Update the item display
        itemDisplay.teleport(location.clone().setDirection(getVelocity()));

        if (!recalled) {
            new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 1.4f, 0.3f).play(getLocation());
            return;
        } else {
            new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 0.2f, 0.3f).play(getLocation());
        }

        // If we're being recalled, move towards the caster
        final Location playerLocation = UtilPlayer.getMidpoint(caster);

        // If we're at the caster, mark for removal
        if (location.distanceSquared(playerLocation) < 1.0) {
            setMarkForRemoval(true);
            return;
        }
        final double speed = getVelocity().length();
        final Vector direction = playerLocation.toVector().subtract(location.toVector()).normalize();
        redirect(direction.multiply(speed));
    }

    public void playRedirectSound() {
        new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 2f, 0.6f).play(getLocation());
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        final Entity entity = result.getHitEntity();
        // If it's not an entity, it's like a block, which means
        // we just want to recall the hammer (like a boomerang, until it expires or is back at the caster)
        if (entity == null) {
            // Recall if not already recalled
            if (!recalled) {
                recall();
                new SoundEffect(Sound.ITEM_TOTEM_USE, 2.0f, 0.1f).play(getLocation());
                Particle.FLASH.builder()
                        .color(Color.WHITE)
                        .count(1)
                        .extra(0.1)
                        .location(location)
                        .receivers(60)
                        .spawn();
                Particle.GUST.builder()
                        .count(1)
                        .extra(0.3)
                        .offset(0.3, 0.3, 0.3)
                        .location(location)
                        .receivers(60)
                        .spawn();
            }
            return CollisionResult.CONTINUE;
        }

        // If it did hit an entity, do damage and deal kb
        // Create damage event
        LivingEntity target = (LivingEntity) entity;
        DamageEvent damageEvent = new DamageEvent(
                target,
                caster,
                itemDisplay,
                new ItemAbilityDamageCause(ability).withBukkitCause(PROJECTILE),
                damage,
                name
        );

        // Apply the damage
        UtilDamage.doDamage(damageEvent);
        if (damageEvent.isCancelled()) {
            return CollisionResult.CONTINUE;
        }

        // Apply custom knockback in the direction the trident is traveling
        Vector knockbackDirection = target.getLocation().subtract(this.location).toVector().normalize();

        // Apply knockback
        VelocityData velocityData = new VelocityData(
                knockbackDirection.multiply(impactVelocity),
                impactVelocity,
                false,
                0.0,
                impactVelocity * 0.5,
                impactVelocity * 0.3,
                true
        );

        UtilVelocity.velocity(target, caster, velocityData, VelocityType.KNOCKBACK);

        // Play hit sound
        new SoundEffect(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 0.1f).play(target.getLocation());
        return CollisionResult.CONTINUE;
    }

    public void remove() {
        itemDisplay.remove();
        Particle.GUST.builder()
                .count(1)
                .extra(0.3)
                .offset(0.3, 0.3, 0.3)
                .location(location)
                .receivers(60)
                .spawn();
        Particle.FLASH.builder()
                .count(1)
                .color(Color.WHITE)
                .extra(0.1)
                .location(location)
                .receivers(60)
                .spawn();
    }
}
