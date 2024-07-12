package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.RayProjectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
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

public final class BloodSphereProjectile extends RayProjectile {

    private static final String NAME = "Blood Sphere";
    private static final long APPLY_INTERVAL = 400L;

    private final Map<UUID, Long> lastApply = new WeakHashMap<>();
    private final List<Location> damageParticles = new ArrayList<>();
    private final Multimap<LivingEntity, Location> healParticles = ArrayListMultimap.create();
    private final double maxHealthPerApply;
    private final double damagePerApply;
    private final double applyRadius;
    private final double impactHealthMultiplier;
    private final double passiveSpeed;
    private final double applySpeed;
    private final double healthSeconds;
    private final double mobHealthModifier;

    private final ChargeData charge;
    private double healthPool;

    public BloodSphereProjectile(@NotNull Player caster,
                                 double hitboxSize,
                                 double size,
                                 Location location,
                                 long expireTime,
                                 float growthPerSecond,
                                 double maxHealthPerSecond,
                                 double damagePerSecond,
                                 double applyRadius,
                                 double impactHealthMultiplier,
                                 double passiveSpeed,
                                 double applySpeed,
                                 double healthSeconds, double mobHealthModifier) {
        super(caster, hitboxSize, size, location, expireTime);
        this.maxHealthPerApply = (APPLY_INTERVAL / 1000d) * maxHealthPerSecond;
        this.damagePerApply = (APPLY_INTERVAL / 1000d) * damagePerSecond;
        this.applyRadius = applyRadius;
        this.impactHealthMultiplier = impactHealthMultiplier;
        this.passiveSpeed = passiveSpeed;
        this.applySpeed = applySpeed;
        this.healthSeconds = healthSeconds;
        this.mobHealthModifier = mobHealthModifier;

        this.charge = new ChargeData(growthPerSecond);
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

        // Change speed based on if we are applying or not
        final List<KeyValue<LivingEntity, EntityProperty>> toApply = UtilEntity.getNearbyEntities(Objects.requireNonNull(this.caster),
                this.location,
                this.applyRadius,
                EntityProperty.ALL);
        toApply.removeIf(kv -> !canApply(kv.getKey()));

        this.setSpeed(toApply.isEmpty() ? this.passiveSpeed : this.applySpeed);

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
        redirect(direction);

        if (location.distanceSquared(centerBody) < 0.3) {
            Particle.SCULK_SOUL.builder()
                    .location(location)
                    .count(10)
                    .extra(0)
                    .offset(1, 1, 1)
                    .receivers(location.getNearbyPlayers(60))
                    .spawn();

            new SoundEffect(Sound.BLOCK_CONDUIT_ACTIVATE, 2f, 1f).play(location);

            final double gained = this.healthPool * this.impactHealthMultiplier;
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
                double toDamage = this.damagePerApply * this.charge.getCharge();
                if (!(entity instanceof Player)) {
                    toDamage *= this.mobHealthModifier;
                }

                final CustomDamageEvent event = new CustomDamageEvent(entity,
                        this.caster,
                        null,
                        EntityDamageEvent.DamageCause.PROJECTILE,
                        toDamage,
                        false,
                        NAME);

                event.setForceDamageDelay(0);
                event.setDoDurability(false);
                UtilDamage.doCustomDamage(event);

                if (!event.isCancelled()) {
                    damageParticles.add(entity.getLocation().add(0, entity.getHeight() / 2d, 0));
                    healthPool += toDamage;
                }
            } else {
                healParticles.put(entity, location.clone());
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

        // Chase entities and heal them
        final Iterator<Map.Entry<LivingEntity, Location>> healIterator = healParticles.entries().iterator();
        while (healIterator.hasNext()) {
            final Map.Entry<LivingEntity, Location> next = healIterator.next();
            final LivingEntity entity = next.getKey();
            if (entity == null || (entity instanceof Player player && !player.isOnline())) {
                healIterator.remove();
                continue;
            }

            // Chase
            final Location point = next.getValue();
            final Location destination = entity.getLocation().add(0, entity.getHeight() / 2d, 0);
            final Location direction = destination.clone().subtract(point);
            direction.multiply(applySpeed * 0.8f);
            Particle.DUST.builder()
                    .data(new Particle.DustOptions(Color.LIME, 0.7f))
                    .location(point.add(direction))
                    .count(1)
                    .extra(0)
                    .receivers(nearby)
                    .spawn();

            // Heal
            if (point.distanceSquared(destination) < 0.3) {
                final double toHeal = Math.min(this.maxHealthPerApply, this.healthPool);

                if (entity instanceof Player player && toHeal > 0) {
                    UtilPlayer.slowHealth(JavaPlugin.getPlugin(Champions.class), player, toHeal, (int) (healthSeconds * 20), true);
                    this.healthPool -= toHeal;
                }

                healIterator.remove();
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
        setSpeed(passiveSpeed);
        Particle.LARGE_SMOKE.builder()
                .location(location)
                .count(20)
                .extra(0)
                .offset(1, 1,  1)
                .receivers(location.getNearbyPlayers(60))
                .spawn();
    }
}
