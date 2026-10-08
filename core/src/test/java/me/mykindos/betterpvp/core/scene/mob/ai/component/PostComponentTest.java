package me.mykindos.betterpvp.core.scene.mob.ai.component;

import com.destroystokyo.paper.entity.Pathfinder;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.ai.FakeComponent;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostComponentTest {

    private final MobFixture fixture = new MobFixture();
    private final AtomicReference<Location> post = new AtomicReference<>(fixture.at(20, 64, 0));
    private final AtomicInteger asked = new AtomicInteger();
    private final FakeComponent lower = new FakeComponent("lower", AIControl.MOVE);
    private PostComponent component;

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob mob() {
        return mob(created -> { });
    }

    /** A mob whose post sits below the components {@code above} adds. */
    private TestMob mob(Consumer<TestMob> above) {
        final TestMob mob = fixture.spawn("knight", created -> {
            above.accept(created);
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
            created.setAnimation(MobAnimation.WORK, "work");
            component = new PostComponent(created, () -> {
                asked.incrementAndGet();
                return Optional.ofNullable(post.get());
            }, fixture.clock()).rest(1000, 2000);
            created.getAi().add(component);
            created.getAi().add(lower);
        });
        fixture.watcher();
        return mob;
    }

    private void hasPath(boolean path) {
        when(fixture.pathfinder.getCurrentPath()).thenReturn(path ? mock(Pathfinder.PathResult.class) : null);
    }

    @Test
    void ac29_claimsMove() {
        mob();

        assertEquals(EnumSet.of(AIControl.MOVE), component.getControls());
    }

    @Test
    void ac29_walksToThePostWhileThereIsOne() {
        final TestMob mob = mob();
        hasPath(true);

        mob.tick();

        verify(fixture.pathfinder).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        assertEquals(0, lower.starts);
    }

    @Test
    void ac29_asksForThePostOnlyWhenDecidingWhereToGo() {
        final TestMob mob = mob();
        hasPath(true);
        mob.tick();
        final int afterDeciding = asked.get();

        for (int i = 0; i < 50; i++) {
            fixture.moveBodyTo(fixture.position().add(0.2, 0, 0));
            mob.tick();
        }

        assertTrue(afterDeciding >= 1);
        assertEquals(afterDeciding, asked.get());
    }

    @Test
    void ac29_withoutAPostItLeavesTheMobToTheComponentsBelow() {
        post.set(null);
        final TestMob mob = mob();

        tick(mob, 3);

        assertTrue(lower.isRunning());
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
        assertEquals(1, asked.get(), "asked once, not on every tick");

        post.set(fixture.at(20, 64, 0));
        tick(mob, 3);
        assertEquals(1, asked.get(), "a new post waits for a replan");

        mob.replan();
        mob.tick();
        assertEquals(2, asked.get());
        verify(fixture.pathfinder).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
    }

    @Test
    void withoutAPostItAsksAgainAfterEachRest() {
        post.set(null);
        final TestMob mob = mob();
        mob.tick();
        assertEquals(1, asked.get());

        post.set(fixture.at(20, 64, 0));
        fixture.advance(999);
        tick(mob, 3);
        assertEquals(1, asked.get(), "still resting, so not asking yet");
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());

        fixture.advance(1002);
        mob.tick();

        assertEquals(2, asked.get());
        verify(fixture.pathfinder).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
    }

    @Test
    void ac29_onArrivalItStopsHoldsWorkAndStaysUntilReplanned() {
        final TestMob mob = mob();
        hasPath(true);
        mob.tick();
        final int afterDeciding = asked.get();

        fixture.moveBodyTo(fixture.at(20, 64, 0));
        mob.tick();
        verify(fixture.pathfinder, atLeastOnce()).stopPathfinding();
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("work"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        clearInvocations(fixture.handler);
        fixture.advance(60_000);
        tick(mob, 100);

        assertEquals(0, lower.starts);
        assertEquals(afterDeciding, asked.get());
        verify(fixture.handler, never()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        verify(fixture.pathfinder, times(1)).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac29_whenTheTripGivesUpItRestsThenAsksAgain() {
        final TestMob mob = mob();
        hasPath(false);

        tick(mob, 400);
        verify(fixture.pathfinder, times(20)).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
        final int afterGivingUp = asked.get();
        clearInvocations(fixture.handler);
        fixture.advance(999);
        tick(mob, 5);
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        assertEquals(afterGivingUp, asked.get(), "resting, so not asking yet");

        fixture.advance(1002);
        tick(mob, 2);

        assertTrue(asked.get() > afterGivingUp);
        verify(fixture.pathfinder, times(21)).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
    }

    @Test
    void ac29_aPostInterruptedWhileRestingKeepsItsRestThenAsksAgain() {
        final FakeComponent higher = new FakeComponent("higher", AIControl.MOVE);
        higher.canStart = false;
        final TestMob mob = mob(created -> created.getAi().add(higher));
        hasPath(false);
        tick(mob, 400);
        verify(fixture.pathfinder, times(20)).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
        final int afterGivingUp = asked.get();

        higher.canStart = true;
        mob.tick();
        assertTrue(higher.isRunning());
        higher.canStart = false;
        tick(mob, 2);
        clearInvocations(fixture.handler);
        fixture.advance(999);
        tick(mob, 3);

        assertEquals(0, lower.starts);
        assertEquals(afterGivingUp, asked.get(), "still resting, so not asking yet");
        verify(fixture.pathfinder, times(20)).moveTo(any(Location.class), anyDouble());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        verify(fixture.handler, never()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(1002);
        tick(mob, 2);

        assertTrue(asked.get() > afterGivingUp);
        verify(fixture.pathfinder, times(21)).moveTo(eq(fixture.at(20, 64, 0)), anyDouble());
    }
}
