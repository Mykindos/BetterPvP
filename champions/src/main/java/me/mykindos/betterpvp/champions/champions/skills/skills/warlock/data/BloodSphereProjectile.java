package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

import static me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory.MAGIC;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.PROJECTILE;

public final class BloodSphereProjectile extends Projectile {

    private static final String NAME = "Blood Sphere";
    private static final long APPLY_INTERVAL = 400L;

    private final Map<UUID, Long> lastApply = new WeakHashMap<>();
    private final List<Location> damageParticles = new ArrayList<>();
    private final double maxDamage;
    private final double damagePerApply;
    private final double applyRadius;
    private final double impactHealthMultiplier;
    private final double passiveSpeed;
    private final double applySpeed;
    private final double healthSeconds;
    private final double mobHealthModifier;
    private final Skill skill;

    private final ChargeData charge;
    private double damageDealt;

    public BloodSphereProjectile(@NotNull Player caster,
                                 double hitboxSize,
                                 Location location,
                                 long expireTime,
                                 float growthPerSecond,
                                 double maxDamage,
                                 double damagePerSecond,
                                 double applyRadius,
                                 double impactHealthMultiplier,
                                 double passiveSpeed,
                                 double applySpeed,
                                 double healthSeconds,
                                 double mobHealthModifier,
                                 Skill skill) {
        super(caster, hitboxSize, location, expireTime);
        this.damagePerApply = (APPLY_INTERVAL / 1000d) * damagePerSecond;
        this.maxDamage = maxDamage;
        this.applyRadius = applyRadius;
        this.impactHealthMultiplier = impactHealthMultiplier;
        this.passiveSpeed = passiveSpeed;
        this.applySpeed = applySpeed;
        this.healthSeconds = healthSeconds;
        this.mobHealthModifier = mobHealthModifier;

        this.charge = new ChargeData(growthPerSecond);
        this.skill = skill;
        charge.setSoundInterval(APPLY_INTERVAL);
    }

    @Override
    protected void onTick() {
        // Play travel particles
        final Collection<Player> nearby = this.location.getNearbyPlayers(60);
        for (Location point : interpolateLine(0.2)) {
            playParticle(point, nearby);
        }

        // If it's impacted, chase the caster
        if (impacted) {
            applyEffects(nearby);
            doImpact();
            return;
        }

        // Change speed d on if we are applying or not
        final List<KeyValue<LivingEntity, EntityProperty>> toApply = UtilEntity.getNearbyEntities(Objects.requireNonNull(this.caster),
                this.location,
                this.applyRadius,
                EntityProperty.ALL);
        toApply.removeIf(kv -> !canApply(kv.getKey()));

        final Vector currentDirection = this.velocity.clone().normalize();
        this.redirect(currentDirection.multiply(toApply.isEmpty() ? this.passiveSpeed : this.applySpeed));

        // Sound effects while damaging/healing
        charge.tick();
        if (!toApply.isEmpty()) {
            charge.tickSound(new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 0.2f), this.caster, true);
        }

        new SoundEffect(Sound.BLOCK_WET_GRASS_STEP, 0f, 0.5f).play(location);
        searchNearby(toApply);
        applyEffects(nearby);
    }

    private boolean canApply(@NotNull LivingEntity entity) {
        final Location location = entity.getLocation().add(0, entity.getHeight() / 2d, 0);
        final RayTraceResult result = this.location.getWorld().rayTraceBlocks(this.location,
                location.clone().subtract(this.location).toVector().normalize(),
                this.location.distance(location),
                FluidCollisionMode.NEVER,
                true);

        return result == null || result.getHitBlock() == null;
    }

    private void playParticle(Location point, Collection<Player> nearby) {
        final Color color = Math.random() > 0.5 ? Color.fromRGB(122, 6, 16) : Color.fromRGB(150, 6, 18);
        Particle.DUST_COLOR_TRANSITION.builder()
                .location(point)
                .data(new Particle.DustTransition(color, Color.BLACK, 0.5f + 5.0f * this.charge.getCharge()))
                .receivers(nearby)
                .spawn();
    }

    private void doImpact() {
        if (caster == null || !caster.isOnline()) {
            return;
        }

        if (caster.getWorld() != location.getWorld()) {
            markForRemoval = true;
            return;
        }

        // Make it chase the caster constantly
        final Location centerBody = caster.getLocation().add(0, caster.getHeight() / 2d, 0);
        final Vector direction = centerBody.clone().subtract(location).toVector();
        redirect(direction.normalize().multiply(passiveSpeed));

        if (location.distanceSquared(centerBody) < 0.3) {
            Particle.SCULK_SOUL.builder()
                    .location(location)
                    .count(10)
                    .extra(0)
                    .offset(1, 1, 1)
                    .receivers(location.getNearbyPlayers(60))
                    .spawn();

            new SoundEffect(Sound.BLOCK_CONDUIT_ACTIVATE, 2f, 1f).play(location);

            final double gained = this.damageDealt * this.impactHealthMultiplier;
            UtilPlayer.slowHealth(JavaPlugin.getPlugin(Champions.class), caster, gained, (int) (healthSeconds * 20), true);
            UtilMessage.message(caster, NAME, "You gained <alt2>%s</alt2> health.", UtilFormat.formatNumber(gained));
            this.markForRemoval = true;
        }
    }

    private void searchNearby(List<KeyValue<LivingEntity, EntityProperty>> toApply) {
        // Damage nearby enemies and queue healing for friendly entities
        final Iterator<KeyValue<LivingEntity, EntityProperty>> iterator = toApply.iterator();
        while (iterator.hasNext()) {
            final KeyValue<LivingEntity, EntityProperty> next = iterator.next();
            final LivingEntity entity = next.getKey();

            // Don't apply if we've already applied recently
            if (UtilTime.elapsed(lastApply.getOrDefault(entity.getUniqueId(), 0L), APPLY_INTERVAL)) {
                lastApply.put(entity.getUniqueId(), System.currentTimeMillis());
            } else {
                continue;
            }

            if (next.getValue() != EntityProperty.FRIENDLY) { // Damage
                final double realDamage = Math.min(maxDamage - damageDealt, this.damagePerApply * this.charge.getCharge());
                if (realDamage <= 0) {
                    continue;
                }

                double toDamage = realDamage;
                if (!(entity instanceof Player)) {
                    toDamage *= this.mobHealthModifier;
                }

                final DamageEvent event = new DamageEvent(entity,
                        this.caster,
                        null,
                        new SkillDamageCause(skill).withCategory(MAGIC).withBukkitCause(PROJECTILE),
                        toDamage,
                        NAME);

                event.setForceDamageDelay(0);
                event.getDurabilityParameters().disableAttackerDurability();
                UtilDamage.doDamage(event);

                if (!event.isCancelled()) {
                    damageParticles.add(entity.getLocation().add(0, entity.getHeight() / 2d, 0));
                    damageDealt += realDamage;
                }
            }

            iterator.remove();
        }
    }

    private void applyEffects(Collection<Player> nearby) {
        // Damage particles chase back to the sphere
        final Iterator<Location> damageIterator = damageParticles.iterator();
        while (damageIterator.hasNext()) {
            final Location point = damageIterator.next();
            final Location direction = this.location.clone().subtract(point);
            direction.multiply(applySpeed * 0.8f);
            Particle.DUST.builder()
                    .data(new Particle.DustOptions(Color.MAROON, 0.7f))
                    .location(point.add(direction))
                    .count(1)
                    .extra(0)
                    .receivers(nearby)
                    .spawn();

            if (point.distanceSquared(this.location) < 0.3) {
                damageIterator.remove();
            }
        }
    }

    @Override
    protected CollisionResult onCollide(RayTraceResult result) {
        return CollisionResult.REFLECT_BLOCKS;
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // Remove if expired
        if (isExpired()) {
            markForRemoval = true;
            return;
        }

        new SoundEffect(Sound.BLOCK_CONDUIT_ACTIVATE, 2f, 1f).play(location);
        Particle.LARGE_SMOKE.builder()
                .location(location)
                .count(20)
                .extra(0)
                .offset(1, 1, 1)
                .receivers(location.getNearbyPlayers(60))
                .spawn();
    }
}