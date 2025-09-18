package me.mykindos.betterpvp.champions.item.bloomrot;

import lombok.Getter;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;

@Getter
public class NectarOfDecayProjectile extends Projectile {

    private final ItemAbility ability;
    private final int poisonAmplifier;
    private final long poisonMillis;
    private final EffectManager effectManager;
    private double cloudRadius;
    private double fallHeight;
    private double coneRadius;
    private double maxConeRadius;
    private final double maxCloudRadius;
    private final long cloudDuration;

    public NectarOfDecayProjectile(Player caster, final Location location, double hitboxSize, long expireTime, int poisonAmplifier, long poisonMillis, double cloudRadius, long cloudDuration, ItemAbility ability, EffectManager effectManager) {
        super(caster, hitboxSize, location, expireTime);
        this.poisonAmplifier = poisonAmplifier;
        this.poisonMillis = poisonMillis;
        this.ability = ability;
        this.effectManager = effectManager;
        this.maxCloudRadius = cloudRadius;
        this.cloudDuration = cloudDuration;
        this.fallHeight = 5;
        this.coneRadius = 1.2;
        this.maxConeRadius = cloudRadius;
        this.gravity = DEFAULT_GRAVITY;
        this.dragCoefficient = DEFAULT_DRAG_COEFFICIENT * 10;
    }

    @Override
    public boolean isExpired() {
        return impacted ? UtilTime.elapsed(impactTime, cloudDuration) : super.isExpired();
    }

    @Override
    protected void onTick() {
        if (!impacted) {
            for (Location point : interpolateLine()) {
                // Play travel particles
                final Color color = Math.random() > 0.5 ? Color.fromRGB(39, 255, 15) : Color.fromRGB(20, 201, 0);
                Particle.DUST.builder()
                        .location(point)
                        .count(1)
                        .extra(0.5)
                        .data(new Particle.DustOptions(color, 1.5f))
                        .receivers(60)
                        .spawn();
            }
            return;
        }

        // We've already hit the ground, start spreading
        if (!location.getBlock().isPassable()) {
            new SoundEffect(Sound.ENTITY_TURTLE_SWIM, 0F, 2.0f).play(getLocation());

            redirect(new Vector());
            gravity = new Vector();
            cloudRadius = Math.min(maxCloudRadius, cloudRadius + 0.1);

            final Location center = getLocation().clone().add(0, cloudRadius / 2, 0);
            // random dots around
            final Collection<Player> nearby = center.getNearbyPlayers(60);
            for (int i = 0; i < cloudRadius * 10; i++) {
                final Color color = Math.random() > 0.5 ? Color.fromRGB(39, 255, 15) : Color.fromRGB(0, 181, 24);
                Particle.DUST.builder()
                        .location(center.clone().add(
                                (Math.random() * 2f - 1f) * cloudRadius,
                                (Math.random() * 2f - 1f) * cloudRadius / 2,
                                (Math.random() * 2f - 1f) * cloudRadius
                        ))
                        .extra(0)
                        .count(1)
                        .offset(0.8f, 0.8f, 0.8f)
                        .data(new Particle.DustOptions(color, 1.9f))
                        .receivers(nearby)
                        .spawn();
            }

            final Collection<LivingEntity> entities = center.getNearbyLivingEntities(cloudRadius);
            for (LivingEntity entity : entities) {
                if (!canCollideWith(entity)) {
                    continue;
                }

                if (effectManager.hasEffect(entity, EffectTypes.POISON, ability.getName())) {
                    continue;
                }

                effectManager.addEffect(entity,
                        caster,
                        EffectTypes.POISON,
                        ability.getName(),
                        poisonAmplifier,
                        poisonMillis,
                        true);
            }
            return;
        }

        // Cylindrical cone trail around the projectile path
        // If it hits the ground, spread
        final Location direction = lastLocation.clone().subtract(location);
        for (Location particlePoint : UtilLocation.getCircumference(getLocation(), coneRadius, (int) (coneRadius * 10))) {
            final Color color = Math.random() > 0.5 ? Color.fromRGB(39, 255, 15) : Color.fromRGB(194, 255, 202);

            for (Location point : VectorLine.withStepSize(particlePoint, particlePoint.clone().add(direction), 0.2).toLocations()) {
                Particle.DUST.builder()
                        .location(point)
                        .count(1)
                        .extra(0.5)
                        .data(new Particle.DustOptions(color, 1f))
                        .receivers(60)
                        .spawn();
            }
        }
        coneRadius = Math.min(coneRadius + 0.4, maxConeRadius);
    }

    @Override
    protected boolean canCollideWith(Entity entity) {
        if (!super.canCollideWith(entity)) {
            return false;
        }

        final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(caster, (LivingEntity) entity);
        event.callEvent();
        return event.getResult() != Event.Result.DENY;
    }

    @Override
    public void redirect(Vector vector) {
        super.redirect(vector);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        new SoundEffect(Sound.BLOCK_GRASS_BREAK, 1f, 1f).play(location);
        location.add(0, 0.5, 0);
        RayTraceResult trace = location.getWorld().rayTraceBlocks(location, new Vector(0, 1, 0), fallHeight);
        if (trace == null) {
            this.location.add(0, fallHeight, 0);
        } else {
            this.location = trace.getHitPosition().toLocation(location.getWorld()).subtract(0, 0.5, 0);
        }

        new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 2.0f).play(getLocation());
        final double speed = getVelocity().length() / 1.5;
        redirect(new Vector(0, -speed, 0));
        this.gravity = DEFAULT_GRAVITY;
        this.dragCoefficient = 0;
    }

}
