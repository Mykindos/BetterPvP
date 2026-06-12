package me.mykindos.betterpvp.champions.item.projectile;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.Projectile;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public class HealingNova extends Projectile {

    private final double healRadius;
    private final double aliveSeconds;
    private final double expandSeconds;
    private final double healAmount;
    private ChargeData chargeData;
    private List<Location> sphere;
    private final double radius;

    private final Set<UUID> healed = new HashSet<>();

    public HealingNova(Player caster, final Location location, double hitboxSize, double healRadius, double aliveSeconds, double expandSeconds, long expireTime, double radius, double healAmount) {
        super(caster, hitboxSize, location, expireTime);
        this.healRadius = healRadius;
        this.aliveSeconds = aliveSeconds;
        this.expandSeconds = expandSeconds;
        this.radius = radius;
        this.healAmount = healAmount;
    }

    @Override
    protected void onTick() {
        if (!impacted) {
            // Travel particles — warm golden/green tones to distinguish from the black hole
            for (Location point : interpolateLine()) {
                Particle.DUST.builder()
                        .location(point)
                        .count(1)
                        .extra(0.5)
                        .offset(0.1, 0.1, 0.1)
                        .data(new Particle.DustOptions(Color.fromRGB(80, 200, 80), 2))
                        .receivers(60)
                        .spawn();
            }
            return;
        }

        if (UtilTime.elapsed(impactTime, (long) ((expandSeconds + aliveSeconds) * 1000L))) {
            markForRemoval = true;
            new SoundEffect(Sound.BLOCK_BEACON_DEACTIVATE, 0f, 1f).play(location);
        } else {
            final float charge = chargeData.getCharge();
            chargeData.tick();
            chargeData.tickSound(new SoundEffect(Sound.BLOCK_BEACON_AMBIENT, 2f, 1.5f), location, true);

            var particleReceivers = location.getNearbyPlayers(60);

            // Expanding ring of green/gold particles
            for (Location point : sphere) {
                final Location direction = location.clone().subtract(point);
                direction.multiply(1 - charge);
                final Color color = Math.random() > 0.5 ? Color.fromRGB(50, 180, 50) : Color.fromRGB(180, 220, 80);
                Particle.DUST.builder()
                        .data(new Particle.DustOptions(color, 2))
                        .location(point.clone().add(direction))
                        .count(1)
                        .extra(0)
                        .receivers(particleReceivers)
                        .spawn();
            }

            Particle.HEART.builder()
                    .location(location)
                    .count(2)
                    .extra(0)
                    .offset(0.5, 0.5, 0.5)
                    .receivers(particleReceivers)
                    .spawn();

            // Heal nearby allies (caster + friendly players) — only once per entity
            for (KeyValue<LivingEntity, EntityProperty> nearbyEnt : UtilEntity.getNearbyEntities(caster, location, healRadius, EntityProperty.FRIENDLY)) {
                LivingEntity entity = nearbyEnt.getKey();
                if (healed.contains(entity.getUniqueId())) continue;
                healed.add(entity.getUniqueId());
                applyHeal(entity);
            }

            // Also heal the caster once
            if (caster != null && !healed.contains(caster.getUniqueId()) && caster.getLocation().distance(location) <= healRadius) {
                healed.add(caster.getUniqueId());
                applyHeal(caster);
            }
        }
    }

    private void applyHeal(LivingEntity entity) {
        UtilEntity.health(entity, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
        entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 4, 0.3, 0.3, 0.3, 0);
        new SoundEffect(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 1.5f).play(entity.getLocation());
    }

    @Override
    public void redirect(Vector vector) {
        super.redirect(vector);
        new SoundEffect(Sound.BLOCK_BEACON_ACTIVATE, 0f, 1f).play(location);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        sphere = UtilLocation.getSphere(location, radius, 6);
        chargeData = new ChargeData((float) (1 / expandSeconds));
        redirect(new Vector());
    }
}
