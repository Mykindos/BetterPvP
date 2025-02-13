package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.Projectile;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PestilenceProjectile extends Projectile {

    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final EffectManager effectManager;
    private final double radius;
    private final double poisonDuration;
    private final int poisonLevel;
    private long lastTargetTime;

    public PestilenceProjectile(@Nullable Player caster, double hitboxSize, Location location, long aliveTime, EffectManager effectManager, double radius, double poisonDuration, int poisonLevel) {
        super(caster, hitboxSize, location, aliveTime);
        this.lastTargetTime = getCreationTime();
        this.effectManager = effectManager;
        this.radius = radius;
        this.poisonDuration = poisonDuration;
        this.poisonLevel = poisonLevel;
    }

    @Override
    public boolean isExpired() {
        return UtilTime.elapsed(lastTargetTime, aliveTime);
    }

    @Override
    protected void onTick() {
        // Play travel particles
        final Collection<Player> receivers = location.getNearbyPlayers(60);
        for (Location point : interpolateLine()) {
            // Play travel particles
            Particle.DUST.builder()
                    .location(point)
                    .count(1)
                    .extra(0.5)
                    .offset(0.1, 0.1, 0.1)
                    .data(new Particle.DustOptions(Color.fromRGB(0, (int) (Math.random() * 100 + 155), 0), (float) this.hitboxSize * 2))
                    .receivers(receivers)
                    .spawn();
        }

        new SoundEffect(Sound.ENTITY_ARMADILLO_BRUSH, 0f, 1f).play(location);
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
    protected void onImpact(Location location, RayTraceResult result) {
        // we hit a block as per #onCollide
        Particle.ITEM_SLIME.builder()
                .location(location)
                .count(50)
                .offset(0.5, 0.5, 0.5)
                .extra(0.5)
                .receivers(location.getNearbyPlayers(60))
                .spawn();
        new SoundEffect(Sound.BLOCK_GRASS_PLACE, 0.5f, 1f).play(location);
        new SoundEffect(Sound.BLOCK_GRASS_PLACE, 1.5f, 1f).play(location);
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        final Entity hitEntity = result.getHitEntity();
        if (hitEntity == null) {
            this.markForRemoval = true; // we hit a block
            return CollisionResult.IMPACT;
        }

        // Apply poison
        LivingEntity livingEntity = (LivingEntity) hitEntity;
        hitEntities.add(livingEntity);
        new SoundEffect(Sound.ENTITY_SILVERFISH_DEATH, 1f, 0.5f).play(livingEntity.getLocation());
        new SoundEffect(Sound.ENTITY_SILVERFISH_DEATH, 1f, 0.5f).play(location);
        this.effectManager.addEffect(livingEntity,
                caster,
                EffectTypes.POISON,
                "Pestilence",
                poisonLevel,
                (long) (poisonDuration * 1000));

        // Redirect if possible, otherwise, remove
        final List<LivingEntity> nearby = UtilEntity.getNearbyEnemies(caster, location, radius);
        final Optional<LivingEntity> closest = nearby.stream()
                .filter(entity -> !hitEntities.contains(entity))
                .filter(entity -> !entity.equals(caster))
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(location)));

        Particle.TRIAL_OMEN.builder()
                .location(livingEntity.getLocation().add(0, livingEntity.getHeight() / 2, 0))
                .count(10)
                .offset(0.5, 0.5, 0.5)
                .extra(0.5)
                .receivers(location.getNearbyPlayers(60))
                .spawn();

        if (closest.isPresent()) {
            final LivingEntity target = closest.get();
            final Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
            final Vector direction = targetLoc.toVector().subtract(location.toVector());
            redirect(direction); // this normalizes it, don't do it twice for performance
            lastTargetTime = System.currentTimeMillis();
        } else {
            this.markForRemoval = true;
        }

        return CollisionResult.CONTINUE; // Continue so it doesn't stop the ray
    }
}
