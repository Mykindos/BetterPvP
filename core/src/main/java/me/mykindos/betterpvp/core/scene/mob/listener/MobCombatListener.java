package me.mykindos.betterpvp.core.scene.mob.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Feeds the {@link me.mykindos.betterpvp.core.scene.mob.target.ThreatTable} of a {@link SceneMob}
 * whenever it takes damage from another entity, so threat-based targeting reflects who has been
 * hurting the mob most. Projectile damage is attributed to the shooter rather than the projectile.
 * <p>
 * Auto-registered by the module's {@code ListenerLoader} via {@link BPvPListener}; no manual wiring
 * is required.
 */
@BPvPListener
@Singleton
public class MobCombatListener implements Listener {

    private final SceneObjectRegistry registry;

    @Inject
    private MobCombatListener(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    /**
     * On a custom mob taking damage, credits the attacking living entity with threat equal to the
     * final damage dealt.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamagee() instanceof LivingEntity victim)) {
            return;
        }

        final SceneMob mob = registry.getObject(victim, SceneMob.class);
        if (mob == null) {
            return;
        }

        final LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        mob.getThreat().add(attacker, event.getDamage());
        mob.getAnimations().play(MobAnimation.HURT);
        mob.getSounds().play(MobSound.HURT);

        final ModeledEntity modeledEntity = mob.getModeledEntity();
        if (modeledEntity != null) {
            modeledEntity.markHurt();
        }
    }

    /**
     * Resolves the living entity responsible for the damage: the projectile's shooter for projectile
     * damage, the damager itself if it is living, or {@code null} when neither applies.
     */
    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile projectile) {
            final ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof LivingEntity living ? living : null;
        }
        return damager instanceof LivingEntity living ? living : null;
    }

}
