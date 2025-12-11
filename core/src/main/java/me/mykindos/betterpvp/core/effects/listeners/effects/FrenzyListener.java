package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.effects.types.negative.FrenzyEffect;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@BPvPListener
public class FrenzyListener implements Listener {

    private final EffectManager effectManager;
    private final Map<Effect, Integer> hitCounter = new ConcurrentHashMap<>();

    @Inject
    private FrenzyListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    // Stop knockback on both players
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(CustomEntityVelocityEvent event) {
        final Entity rawEntity = event.getEntity();
        if (!(rawEntity instanceof LivingEntity entity)) return;

        if (FrenzyEffect.isFrenzy(entity)) {
            event.setCancelled(true); // This works on APPLIERS and AFFECTED entities
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!FrenzyEffect.isFrenzy(event.getPlayer())) {
            return;
        }

        final LivingEntity target = FrenzyEffect.getTarget(event.getPlayer());
        if (target != null) {
            effectManager.removeEffect(target, EffectTypes.FRENZY);
        }
    }

    // Stop skill use
    @EventHandler
    public void onSkillUse(PlayerUseSkillEvent event) {
        if (FrenzyEffect.isFrenzy(event.getPlayer()) && isMovementSkill(event.getSkill())) {
            event.setCancelled(true);
            event.setCancelReason("Frenzy");
        }
    }

    // Teleport Around and do sounds
    @EventHandler
    public void onAttack(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!event.isDamageeLiving()) return;

        final LivingEntity damager = event.getDamager();
        final LivingEntity damagee = Objects.requireNonNull(event.getLivingDamagee());
        if (damager == null) return;
        if (!FrenzyEffect.isFrenzy(damager) || !FrenzyEffect.isFrenzy(damagee)) return;
        if (!effectManager.hasEffect(damagee, EffectTypes.FRENZY)) return;

        // Now that they're both frenzy, teleport the damager around the damagee after you deal this hit
        final Effect effect = effectManager.getEffect(damagee, EffectTypes.FRENZY).orElseThrow();
        final String name = effect.getName().isEmpty() ? EffectTypes.FRENZY.getName() : effect.getName();

        // Check hit count against the effect level (max hits allowed)
        final int currentHits = hitCounter.getOrDefault(effect, 0);
        final int maxHits = effect.getAmplifier();

        // If we've already reached the limit, remove the effect and don't process
        if (currentHits >= maxHits) {
            effectManager.removeEffect(damagee, EffectTypes.FRENZY);
            hitCounter.remove(effect);
            return;
        }

        playDamage(damagee, damager);
        playTeleport(damagee, damager);
        FrenzyEffect.teleportAround(damagee, damager);
        event.addReason(name);

        // Increment hit count after processing
        final int newHits = currentHits + 1;
        hitCounter.put(effect, newHits);

        // Check if we've reached the limit after this hit
        if (newHits >= maxHits) {
            effectManager.removeEffect(damagee, EffectTypes.FRENZY);
            hitCounter.remove(effect);
        }
    }

    @EventHandler
    public void onReceive(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() != EffectTypes.FRENZY) {
            return;
        }

        final LivingEntity applier = event.getEffect().getApplier().get();
        if (applier != null) {
            playTeleport(event.getTarget(), applier);
            FrenzyEffect.teleportAround(event.getTarget(), applier);
        }

        // Initialize hit counter for new effect
        hitCounter.put(event.getEffect(), 0);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (FrenzyEffect.isFrenzy(event.getEntity())) {
            // Clear your applier's effect
            if (effectManager.hasEffect(event.getEntity(), EffectTypes.FRENZY)) {
                final Effect effect = effectManager.getEffect(event.getEntity(), EffectTypes.FRENZY).orElseThrow();
                final LivingEntity applier = effect.getApplier().get();
                if (applier != null) {
                    FrenzyEffect.clear(applier);
                }
            }

            // Clear target's effect, if any
            LivingEntity target = FrenzyEffect.getTarget(event.getEntity());
            if (target != null) {
                FrenzyEffect.clear(target);
            }

            FrenzyEffect.clear(event.getEntity());
        }
    }

    @EventHandler
    public void onExpire(EffectExpireEvent event) {
        hitCounter.remove(event.getEffect());
    }

    private void playTeleport(LivingEntity damagee, LivingEntity damager) {
        new SoundEffect("littleroom_kurrot", "littleroom.kurrot.ranged_slash_swing1", 2f, 1.8f).play(damagee.getLocation());

        Particle.CLOUD.builder()
                .location(damager.getLocation())
                .offset(0.5, 0.5, 0.5)
                .extra(0)
                .count(10)
                .receivers(60)
                .spawn();
    }

    private void playDamage(LivingEntity damagee, LivingEntity damager) {
        new SoundEffect("littleroom_halloween2", "littleroom.scarecrow.slash", 2f, 1.5f).play(damagee.getLocation());
        new SoundEffect(Sound.BLOCK_STONE_BREAK, 0.8f, 1f).play(damagee.getLocation());

        Particle.BLOCK_CRUMBLE.builder()
                .data(Material.RED_CONCRETE.createBlockData())
                .offset(0.5, 0.5, 0.5)
                .location(damagee.getLocation())
                .count(50)
                .receivers(60)
                .spawn();

        final Vector distance = damagee.getLocation().toVector().subtract(damager.getLocation().toVector());
        distance.normalize();
        distance.multiply(FrenzyEffect.REACH - 1);
        final Location location = damager.getEyeLocation().add(distance);
        Particle.SWEEP_ATTACK.builder()
                .location(location)
                .offset(0.5, 0.5, 0.5)
                .count(4)
                .receivers(60)
                .spawn();

        playSwingArc(damagee, damager);
    }

    private void playSwingArc(LivingEntity damagee, LivingEntity damager) {
        Location origin = damager.getEyeLocation();
        Location targetCenter = damagee.getLocation().add(0, damagee.getHeight() / 2, 0);

        Vector forward = targetCenter.toVector().subtract(origin.toVector()).normalize();

        // Build perpendicular basis vectors
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        if (right.lengthSquared() < 0.001) {
            right = new Vector(1, 0, 0);
            up = new Vector(0, 0, 1);
        }

        double arcRadius = 1.2;
        double arcAngle = Math.toRadians(100);
        int particleCount = 12;

        // Random swing direction (perpendicular to forward)
        double randomRotation = Math.random() * 2 * Math.PI / 2;
        Vector swingAxis = right.clone().rotateAroundAxis(forward, randomRotation);

        // Arc center at midpoint
        double distanceInFront = FrenzyEffect.REACH / 3;
        Location arcCenter = origin.clone().add(forward.clone().multiply(distanceInFront));

        // Draw arc in the plane of (swingAxis, forward)
        // Arc bulges toward damagee, opens toward damager
        double halfAngle = arcAngle / 2;
        for (int i = 0; i <= particleCount; i++) {
            double t = (double) i / particleCount;
            double angle = -halfAngle + (arcAngle * t);

            // angle=0 points toward damagee (middle of swing)
            // angle=+-halfAngle are the endpoints (to the sides)
            double swingComponent = Math.sin(angle);
            double forwardComponent = Math.cos(angle);

            Vector offset = swingAxis.clone().multiply(swingComponent * arcRadius)
                    .add(forward.clone().multiply(forwardComponent * arcRadius));

            Location particleLoc = arcCenter.clone().add(offset);

            Particle.DUST.builder()
                    .location(particleLoc)
                    .data(new Particle.DustOptions(Color.fromRGB(100, 250, 50), 1.2f))
                    .count(1)
                    .receivers(60)
                    .spawn();
        }
    }

    // fuck everyone
    private boolean isMovementSkill(IChampionsSkill skill) {
        final Class<? extends IChampionsSkill> clazz = skill.getClass();
        for (Class<?> superClass : clazz.getInterfaces()) {
            if (superClass.getName().equals("me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill")) {
                return true;
            }
        }
        return false;
    }

}
