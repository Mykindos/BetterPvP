package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.sound.MobSound;
import me.mykindos.betterpvp.core.scene.mob.sound.SoundProvider;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeleeAttackComponentTest {

    private final MobFixture fixture = new MobFixture();
    private final SoundProvider attackSound = mock(SoundProvider.class);
    private final TestMob mob = fixture.spawn("knight", created -> {
        created.setAnimation(MobAnimation.IDLE, "idle");
        created.setAnimation(MobAnimation.WALK, "walk");
        created.setAnimation(MobAnimation.ATTACK, "attack");
        created.sound(MobSound.ATTACK, attackSound);
    });
    private final List<List<?>> hits = new ArrayList<>();
    private final MockedConstruction<DamageEvent> damageEvents =
            mockConstruction(DamageEvent.class, (event, context) -> hits.add(context.arguments()));
    private final MockedStatic<UtilDamage> damage = mockStatic(UtilDamage.class);
    private final LivingEntity target = fixture.living(near());

    @AfterEach
    void close() {
        damage.close();
        damageEvents.close();
        fixture.close();
    }

    /** Inside the mob's box grown by 2.5. */
    private Location near() {
        return fixture.at(3.0, 64, 0.5);
    }

    /** Just outside the mob's box grown by 2.5. */
    private Location far() {
        return fixture.at(5.0, 64, 0.5);
    }

    private MeleeAttackComponent melee() {
        final MeleeAttackComponent melee = new MeleeAttackComponent(mob, fixture.clock());
        mob.setCurrentTarget(target);
        return melee;
    }

    @Test
    void ac16_runsWhileTheMobHasAValidTarget() {
        final MeleeAttackComponent melee = new MeleeAttackComponent(mob, fixture.clock());
        assertFalse(melee.canStart());

        mob.setCurrentTarget(target);
        assertTrue(melee.canStart());
        assertTrue(melee.shouldContinue());

        when(target.isDead()).thenReturn(true);
        assertFalse(melee.shouldContinue());
    }

    @Test
    void ac16_chasesATargetOutOfReach() {
        final MeleeAttackComponent melee = melee();
        fixture.move(target, far());

        melee.tick();

        verify(fixture.pathfinder).moveTo(far(), 1.0);
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
        assertTrue(hits.isEmpty());
    }

    @Test
    void ac16_swingsWithinReachPlayingAttackAndTheAttackSound() {
        final MeleeAttackComponent melee = melee();

        melee.tick();

        assertEquals(1, hits.size());
        assertEquals(target, hits.getFirst().get(0));
        assertEquals(fixture.body, hits.getFirst().get(1));
        assertEquals(4.0, hits.getFirst().get(hits.getFirst().size() - 1));
        damage.verify(() -> UtilDamage.doDamage(damageEvents.constructed().getFirst()));
        verify(fixture.handler).playAnimation("attack", 0.2, 0.2, 1.0, true);
        verify(attackSound).resolve(mob);
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac16_swingsOnItsCooldown() {
        final MeleeAttackComponent melee = melee();

        melee.tick();
        melee.tick();
        fixture.advance(399);
        melee.tick();
        assertEquals(1, hits.size());

        fixture.advance(1);
        melee.tick();
        assertEquals(2, hits.size());
    }

    @Test
    void ac16_staysRootedForTheCooldownAfterASwing() {
        final MeleeAttackComponent melee = melee();
        melee.tick();
        fixture.move(target, far());

        fixture.advance(100);
        melee.tick();
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
        verify(fixture.pathfinder).stopPathfinding();

        fixture.advance(300);
        melee.tick();
        verify(fixture.pathfinder).moveTo(far(), 1.0);
    }

    @Test
    void ac16_aSetFreezeTimeReplacesTheCooldown() {
        final MeleeAttackComponent melee = melee().freezeMillis(1000L).cooldownMillis(200);
        melee.tick();
        fixture.move(target, far());

        fixture.advance(500);
        melee.tick();
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());

        fixture.advance(500);
        melee.tick();
        verify(fixture.pathfinder).moveTo(far(), 1.0);
    }

    @Test
    void ac16_aFreezeOfZeroTurnsRootingOff() {
        final MeleeAttackComponent melee = melee().freezeMillis(0L);
        melee.tick();
        fixture.move(target, far());

        fixture.advance(100);
        melee.tick();

        verify(fixture.pathfinder).moveTo(far(), 1.0);
    }

    @Test
    void ac16_aWindUpDelaysTheHit() {
        final MeleeAttackComponent melee = melee().windupMillis(200);

        melee.tick();
        verify(fixture.handler).playAnimation("attack", 0.2, 0.2, 1.0, true);
        assertTrue(hits.isEmpty());
        fixture.advance(199);
        melee.tick();
        assertTrue(hits.isEmpty());

        fixture.advance(1);
        melee.tick();
        assertEquals(1, hits.size());
    }

    @Test
    void ac16_aDelayedHitMissesATargetThatDied() {
        final MeleeAttackComponent melee = melee().windupMillis(200);
        melee.tick();

        when(target.isDead()).thenReturn(true);
        fixture.advance(200);
        melee.tick();

        assertTrue(hits.isEmpty());
    }

    @Test
    void ac16_aDelayedHitMissesATargetThatLeftReach() {
        final MeleeAttackComponent melee = melee().windupMillis(200);
        melee.tick();

        fixture.move(target, far());
        fixture.advance(200);
        melee.tick();

        assertTrue(hits.isEmpty());
    }

    @Test
    void ac16_stoppingDropsThePendingHitAndPlaysIdle() {
        final MeleeAttackComponent melee = melee().windupMillis(200).cooldownMillis(10_000).freezeMillis(0L);
        melee.tick();

        melee.stop();
        fixture.advance(300);
        melee.tick();

        assertTrue(hits.isEmpty());
        verify(fixture.pathfinder, times(1)).stopPathfinding();
        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac16_theDamageCanBeChanged() {
        melee().damage(9).tick();

        assertEquals(9.0, hits.getFirst().get(hits.getFirst().size() - 1));
    }
}
