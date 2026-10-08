package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuiltInComponentsTest {

    private final MobFixture fixture = new MobFixture();
    private final TestMob mob = fixture.spawn("knight", created -> {
        created.setAnimation(MobAnimation.IDLE, "idle");
        created.setAnimation(MobAnimation.WALK, "walk");
    });

    @AfterEach
    void close() {
        fixture.close();
    }

    @Test
    void ac11_movementComponentsClaimOnlyMove() {
        assertEquals(EnumSet.of(AIControl.MOVE), new WanderComponent(mob).getControls());
        assertEquals(EnumSet.of(AIControl.MOVE), new ReturnHomeComponent(mob).getControls());
        assertEquals(EnumSet.of(AIControl.MOVE), new FollowOwnerComponent(mob).getControls());
        assertEquals(EnumSet.of(AIControl.MOVE), new MeleeAttackComponent(mob).getControls());
    }

    @Test
    void ac11_componentsThatTurnTheHeadClaimOnlyLook() {
        assertEquals(EnumSet.of(AIControl.LOOK), new LookAtTargetComponent(mob).getControls());
    }

    @Test
    void ac12_startsBeyondThirtyBlocksFromHome() {
        final ReturnHomeComponent component = new ReturnHomeComponent(mob);

        fixture.moveBodyTo(fixture.at(30.5, 64, 0.5));
        assertFalse(component.canStart());

        fixture.moveBodyTo(fixture.at(31.5, 64, 0.5));
        assertTrue(component.canStart());
    }

    @Test
    void ac12_startsInAnotherWorld() {
        final ReturnHomeComponent component = new ReturnHomeComponent(mob);

        fixture.moveBodyTo(new Location(fixture.otherWorld, 0.5, 64, 0.5));

        assertTrue(component.canStart());
        assertTrue(component.shouldContinue());
    }

    @Test
    void ac12_dropsTheTargetAndWalksHomeAtFullSpeed() {
        final ReturnHomeComponent component = new ReturnHomeComponent(mob);
        mob.setCurrentTarget(fixture.living(fixture.at(40, 64, 0)));
        fixture.moveBodyTo(fixture.at(35, 64, 0.5));

        component.tick();

        assertNull(mob.getCurrentTarget());
        verify(fixture.pathfinder).moveTo(fixture.at(0.5, 64, 0.5), 1.0);
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac12_keepsWalkingUntilWithinHalfTheRangeThenPlaysIdle() {
        final ReturnHomeComponent component = new ReturnHomeComponent(mob);

        fixture.moveBodyTo(fixture.at(16, 64, 0.5));
        assertTrue(component.shouldContinue());
        fixture.moveBodyTo(fixture.at(15, 64, 0.5));
        assertFalse(component.shouldContinue());

        component.stop();
        verify(fixture.pathfinder).stopPathfinding();
        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac12_theRangeCanBeChanged() {
        final ReturnHomeComponent component = new ReturnHomeComponent(mob).leashRange(10).returnSpeed(1.4);
        fixture.moveBodyTo(fixture.at(11.5, 64, 0.5));

        assertTrue(component.canStart());
        component.tick();
        verify(fixture.pathfinder).moveTo(fixture.at(0.5, 64, 0.5), 1.4);
    }

    private Player owner(double distance) {
        final Player owner = fixture.player(fixture.position().add(distance, 0, 0));
        mob.setOwner(owner.getUniqueId());
        return owner;
    }

    @Test
    void ac13_followsAnOwnerMoreThanFiveBlocksAway() {
        final FollowOwnerComponent component = new FollowOwnerComponent(mob);
        final Player owner = owner(6);

        assertTrue(component.canStart());
        component.tick();

        verify(fixture.pathfinder).moveTo(owner.getLocation(), 1.0);
    }

    @Test
    void ac13_staysPutWithinFiveBlocks() {
        owner(4);

        assertFalse(new FollowOwnerComponent(mob).canStart());
    }

    @Test
    void ac13_neverRunsWhileTheMobHasATarget() {
        owner(10);
        mob.setCurrentTarget(fixture.living(fixture.at(3, 64, 0)));

        assertFalse(new FollowOwnerComponent(mob).canStart());
    }

    @Test
    void ac13_teleportsToAnOwnerBeyondTwentyFiveBlocks() {
        final FollowOwnerComponent component = new FollowOwnerComponent(mob);
        final Player owner = owner(26);

        component.tick();

        verify(fixture.body).teleport(owner.getLocation());
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac13_ignoresAnOfflineOwnerOrOneInAnotherWorld() {
        final Player owner = owner(10);
        when(owner.isOnline()).thenReturn(false);
        assertFalse(new FollowOwnerComponent(mob).canStart());

        when(owner.isOnline()).thenReturn(true);
        fixture.move(owner, new Location(fixture.otherWorld, 10, 64, 0));
        assertFalse(new FollowOwnerComponent(mob).canStart());
    }

    @Test
    void ac13_ignoresAMobWithNoOwner() {
        assertFalse(new FollowOwnerComponent(mob).canStart());
    }

    @Test
    void ac14_setsTheTargetToTheSelectorsPick() {
        final LivingEntity enemy = fixture.living(fixture.at(5, 64, 0));
        final TargetingComponent component = new TargetingComponent(mob, unused -> Optional.of(enemy));

        assertEquals(EnumSet.of(AIControl.TARGET), component.getControls());
        assertTrue(component.canStart());
        component.tick();

        assertEquals(enemy, mob.getCurrentTarget());
    }

    @Test
    void ac14_setsNoTargetWhenTheSelectorPicksNone() {
        mob.setCurrentTarget(fixture.living(fixture.at(5, 64, 0)));

        new TargetingComponent(mob, unused -> Optional.empty()).tick();

        assertNull(mob.getCurrentTarget());
    }

    @Test
    void ac14_dropsADeadInvalidOrFarWorldTargetBeforeSelecting() {
        final List<LivingEntity> seen = new ArrayList<>();
        final TargetingComponent component = new TargetingComponent(mob, selecting -> {
            seen.add(selecting.getCurrentTarget());
            return Optional.empty();
        });
        final LivingEntity dead = fixture.living(fixture.at(5, 64, 0));
        when(dead.isDead()).thenReturn(true);
        final LivingEntity invalid = fixture.living(fixture.at(5, 64, 0));
        when(invalid.isValid()).thenReturn(false);
        final LivingEntity elsewhere = fixture.living(new Location(fixture.otherWorld, 5, 64, 0));
        final LivingEntity alive = fixture.living(fixture.at(5, 64, 0));

        for (LivingEntity target : List.of(dead, invalid, elsewhere, alive)) {
            mob.setCurrentTarget(target);
            component.tick();
        }

        assertEquals(Arrays.asList(null, null, null, alive), seen);
    }

    @Test
    void ac15_runsWhileTheMobHasThreat() {
        final RetaliateComponent component = new RetaliateComponent(mob);
        assertEquals(EnumSet.of(AIControl.TARGET), component.getControls());
        assertFalse(component.canStart());

        mob.getThreat().add(fixture.living(fixture.at(3, 64, 0)), 1);

        assertTrue(component.canStart());
        assertTrue(component.shouldContinue());
    }

    @Test
    void ac15_lowersThreatAndTargetsTheAttackerWithTheMost() {
        final LivingEntity big = fixture.living(fixture.at(3, 64, 0));
        final LivingEntity small = fixture.living(fixture.at(4, 64, 0));
        mob.getThreat().add(big, 10);
        mob.getThreat().add(small, 6);

        new RetaliateComponent(mob).tick();

        assertEquals(9.5, mob.getThreat().get(big));
        assertEquals(5.5, mob.getThreat().get(small));
        assertEquals(big, mob.getCurrentTarget());
    }

    @Test
    void ac15_forgetsAttackersThatAreNoLongerValid() {
        final LivingEntity gone = fixture.living(fixture.at(3, 64, 0));
        when(gone.isValid()).thenReturn(false);
        final LivingEntity other = fixture.living(fixture.at(4, 64, 0));
        mob.getThreat().add(gone, 10);
        mob.getThreat().add(other, 6);
        final RetaliateComponent component = new RetaliateComponent(mob);

        component.tick();
        assertEquals(0.0, mob.getThreat().get(gone));
        component.tick();

        assertEquals(other, mob.getCurrentTarget());
    }

    @Test
    void ac15_clearsTheTargetWhenItStops() {
        mob.setCurrentTarget(fixture.living(fixture.at(3, 64, 0)));

        new RetaliateComponent(mob).stop();

        assertNull(mob.getCurrentTarget());
    }

    @Test
    void ac15_theDecayCanBeChanged() {
        final LivingEntity attacker = fixture.living(fixture.at(3, 64, 0));
        mob.getThreat().add(attacker, 10);

        new RetaliateComponent(mob).threatDecay(4).tick();

        assertEquals(6.0, mob.getThreat().get(attacker));
    }

    @Test
    void ac18_runsWhileTheMobHasATargetAndFacesItsEyes() {
        final LookAtTargetComponent component = new LookAtTargetComponent(mob);
        assertFalse(component.canStart());
        final LivingEntity target = fixture.living(fixture.at(6, 64, 2));
        mob.setCurrentTarget(target);

        assertTrue(component.canStart());
        component.tick();
        component.tick();

        verify(fixture.body, times(2)).lookAt(target.getEyeLocation());
    }
}
