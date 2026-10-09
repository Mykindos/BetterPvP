package me.mykindos.betterpvp.core.scene.mob;

import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.ai.AIController;
import me.mykindos.betterpvp.core.scene.mob.ai.FakeComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.RetaliateComponent;
import me.mykindos.betterpvp.core.scene.mob.animation.AnimationProviders;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Acceptance criteria of #2384. */
class MobAICertificationTest {

    private final MobFixture fixture = new MobFixture();

    @AfterEach
    void close() {
        fixture.close();
    }

    @Test
    void ac1_proximityIsCheckedOnceEveryTwentyTicksExactly() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        final Player player = fixture.player(fixture.position());
        mob.tick();

        fixture.move(player, fixture.position().add(200, 0, 0));
        tick(mob, 19);
        assertTrue(component.isRunning(), "no check between the first tick and the twentieth after it");
        mob.tick();
        assertFalse(component.isRunning(), "the check twenty ticks after the first found nobody");

        fixture.move(player, fixture.position());
        tick(mob, 19);
        assertEquals(1, component.starts, "no check between two samples");
        mob.tick();
        assertEquals(2, component.starts, "the next check twenty ticks later found the player");
    }

    @Test
    void ac2_aMobWhoseBodyWasDeadIsActiveAgainOnTheFirstTickItIsAlive() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        fixture.watcher();
        mob.tick();
        when(fixture.body.isDead()).thenReturn(true);
        tick(mob, 3);
        assertFalse(component.isRunning());

        when(fixture.body.isDead()).thenReturn(false);
        mob.tick();

        assertTrue(mob.isActive());
        assertTrue(component.isRunning());
    }

    @Test
    void ac2_aMobWhoseDeathClipEndedIsActiveAgainOnTheNextTick() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn("knight", created -> created.getAi().add(component));
        fixture.watcher();
        mob.tick();
        when(fixture.handler.isPlayingAnimation("death")).thenReturn(true);
        tick(mob, 3);
        assertFalse(component.isRunning());

        when(fixture.handler.isPlayingAnimation("death")).thenReturn(false);
        mob.tick();

        assertTrue(mob.isActive());
        assertTrue(component.isRunning());
    }

    @Test
    void ac2_aRevivedMobWithNobodyInRangeStaysInactive() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        final Player player = fixture.player(fixture.position());
        mob.tick();
        fixture.move(player, fixture.position().add(200, 0, 0));
        when(fixture.body.isDead()).thenReturn(true);
        tick(mob, 25);

        when(fixture.body.isDead()).thenReturn(false);
        mob.tick();

        assertFalse(mob.isActive());
        assertFalse(component.isRunning());
    }

    @Test
    void ac3_retaliateForgetsAnAttackerItCanNoLongerFind() {
        final TestMob mob = fixture.spawn(created -> { });
        final LivingEntity gone = fixture.living(fixture.at(3, 64, 0));
        final LivingEntity other = fixture.living(fixture.at(4, 64, 0));
        mob.getThreat().add(gone, 10);
        mob.getThreat().add(other, 6);
        fixture.entities.remove(gone.getUniqueId());
        final RetaliateComponent component = new RetaliateComponent(mob);

        component.tick();
        assertEquals(0.0, mob.getThreat().get(gone));
        component.tick();

        assertEquals(other, mob.getCurrentTarget());
    }

    @Test
    void ac3_retaliateClearsTheTargetWhenTheForgottenAttackerWasIt() {
        final TestMob mob = fixture.spawn(created -> { });
        final LivingEntity gone = fixture.living(fixture.at(3, 64, 0));
        mob.getThreat().add(gone, 10);
        mob.setCurrentTarget(gone);
        fixture.entities.remove(gone.getUniqueId());

        new RetaliateComponent(mob).tick();

        assertNotEquals(gone, mob.getCurrentTarget());
    }

    @Test
    void ac3_retaliateClearsTheTargetWhenTheInvalidAttackerWasIt() {
        final TestMob mob = fixture.spawn(created -> { });
        final LivingEntity invalid = fixture.living(fixture.at(3, 64, 0));
        mob.getThreat().add(invalid, 10);
        mob.setCurrentTarget(invalid);
        when(invalid.isValid()).thenReturn(false);

        new RetaliateComponent(mob).tick();

        assertEquals(0.0, mob.getThreat().get(invalid));
        assertNull(mob.getCurrentTarget());
    }

    @Test
    void ac3_retaliateKeepsAnotherTargetWhenForgettingAnAttacker() {
        final TestMob mob = fixture.spawn(created -> { });
        final LivingEntity gone = fixture.living(fixture.at(3, 64, 0));
        final LivingEntity other = fixture.living(fixture.at(4, 64, 0));
        mob.getThreat().add(gone, 10);
        mob.getThreat().add(other, 6);
        mob.setCurrentTarget(other);
        fixture.entities.remove(gone.getUniqueId());

        new RetaliateComponent(mob).tick();

        assertEquals(other, mob.getCurrentTarget());
    }

    @Test
    void ac4_randomAndSequentialRefuseANullClipId() {
        assertThrows(NullPointerException.class, () -> AnimationProviders.random("a", null));
        assertThrows(NullPointerException.class, () -> AnimationProviders.sequential(null, "b"));
    }

    @Test
    void ac5_runningComponentsTickInPriorityOrderHighestFirst() {
        final List<String> log = new ArrayList<>();
        final AIController controller = new AIController();
        final List<String> priority = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            controller.add(new FakeComponent("low" + i, log));
            priority.add("tick low" + i);
        }
        for (int i = 0; i < 8; i++) {
            controller.addFirst(new FakeComponent("high" + i, log));
            priority.addFirst("tick high" + i);
        }
        controller.tick();
        log.clear();

        controller.tick();

        assertEquals(priority, log);
    }

    @Test
    void ac6_attendAndOrderToBeforeSpawnDoNothing() {
        final TestMob mob = fixture.create(null, created -> { });
        final Player player = fixture.player(fixture.at(3, 64, 0));

        assertDoesNotThrow(() -> mob.attend(player));
        assertDoesNotThrow(() -> mob.orderTo(fixture.at(20, 64, 20)));

        mob.init(fixture.newBody());
        fixture.watcher();
        tick(mob, 3);
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
        verify(fixture.body, never()).lookAt(any(Location.class));
    }

    @Test
    void ac6_aNewOrderSpeedAppliesToTheNextOrderWithoutARespawn() {
        final TestMob mob = fixture.spawn(created -> { });
        fixture.watcher();
        mob.tick();

        mob.setOrderSpeed(1.7);
        mob.orderTo(fixture.at(20, 64, 20));
        mob.tick();

        verify(fixture.pathfinder).moveTo(any(Location.class), eq(1.7));
        assertEquals(1, mob.registrations);
    }

    @Test
    void ac7_stoppingAHeldClipBlendsItOutOverAFifthOfASecond() {
        final TestMob mob = fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
        });
        final IAnimationProperty walk = mock(IAnimationProperty.class);
        when(fixture.handler.getAnimation("walk")).thenReturn(walk);
        mob.getAnimations().play(MobAnimation.WALK);

        mob.getAnimations().play(MobAnimation.IDLE);

        final InOrder order = inOrder(walk, fixture.handler);
        order.verify(walk).setLerpOutTime(0.2);
        order.verify(fixture.handler).stopAnimation("walk");
    }

    @Test
    void ac7_aHeldClipThatIsNoLongerPlayingIsStillStopped() {
        final TestMob mob = fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
        });
        mob.getAnimations().play(MobAnimation.WALK);

        assertDoesNotThrow(() -> mob.getAnimations().play(MobAnimation.IDLE));

        verify(fixture.handler).stopAnimation("walk");
    }
}
