package me.mykindos.betterpvp.core.scene.mob.listener;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobCombatListenerTest {

    private final MobFixture fixture = new MobFixture();
    private final TestMob mob = fixture.spawn("knight", created -> created.setAnimation(MobAnimation.HURT, "hurt"));
    private final Mob victim = fixture.body;
    private final SceneObjectRegistry registry = mock(SceneObjectRegistry.class);
    private final MobCombatListener listener = listener(registry);

    MobCombatListenerTest() {
        when(registry.getObject(victim, SceneMob.class)).thenReturn(mob);
    }

    @AfterEach
    void close() {
        fixture.close();
    }

    private static MobCombatListener listener(SceneObjectRegistry registry) {
        try {
            final Constructor<MobCombatListener> constructor = MobCombatListener.class.getDeclaredConstructor(SceneObjectRegistry.class);
            constructor.setAccessible(true);
            return constructor.newInstance(registry);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private DamageEvent damage(Entity damager, double amount, boolean cancelled) {
        final DamageEvent event = mock(DamageEvent.class);
        when(event.getDamagee()).thenReturn(victim);
        when(event.getDamager()).thenReturn(damager instanceof LivingEntity living ? living : null);
        when(event.isCancelled()).thenReturn(cancelled);
        when(event.getDamage()).thenReturn(amount);
        return event;
    }

    @Test
    void ac10_aLivingAttackerGainsThreatEqualToTheFinalDamage() {
        final LivingEntity attacker = fixture.living(fixture.at(2, 64, 0));

        listener.onDamage(damage(attacker, 7.5, false));
        listener.onDamage(damage(attacker, 2.5, false));

        assertEquals(10.0, mob.getThreat().get(attacker));
    }

    @Test
    void ac10_theMobPlaysHurtAndItsModelFlashes() {
        final LivingEntity attacker = fixture.living(fixture.at(2, 64, 0));

        listener.onDamage(damage(attacker, 3, false));

        verify(fixture.handler).playAnimation("hurt", 0.2, 0.2, 1.0, true);
        verify(fixture.modeled).markHurt();
    }

    @Test
    void ac10_aProjectileCreditsTheLivingEntityThatShotIt() {
        final LivingEntity shooter = fixture.living(fixture.at(10, 64, 0));
        final Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(shooter);
        final DamageEvent event = damage(shooter, 4, false);
        when(event.getDamagingEntity()).thenReturn(arrow);
        when(event.getProjectile()).thenReturn(arrow);
        when(event.isProjectile()).thenReturn(true);

        listener.onDamage(event);

        assertEquals(4.0, mob.getThreat().get(shooter));
    }

    @Test
    void ac10_cancelledDamageAddsNoThreat() {
        final LivingEntity attacker = fixture.living(fixture.at(2, 64, 0));

        listener.onDamage(damage(attacker, 6, true));

        assertTrue(mob.getThreat().isEmpty());
        verify(fixture.modeled, never()).markHurt();
        verify(fixture.handler, never()).playAnimation(anyString(), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac10_damageWithoutALivingSourceAddsNoThreat() {
        listener.onDamage(damage(null, 6, false));

        assertTrue(mob.getThreat().isEmpty());
        verify(fixture.modeled, never()).markHurt();
    }
}
