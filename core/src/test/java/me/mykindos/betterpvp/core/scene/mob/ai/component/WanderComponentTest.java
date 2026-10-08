package me.mykindos.betterpvp.core.scene.mob.ai.component;

import com.destroystokyo.paper.entity.Pathfinder;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WanderComponentTest {

    private final MobFixture fixture = new MobFixture();

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob plain() {
        return fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
        });
    }

    private Location lastTrip() {
        final ArgumentCaptor<Location> sent = ArgumentCaptor.forClass(Location.class);
        verify(fixture.pathfinder, atLeastOnce()).moveTo(sent.capture(), anyDouble());
        return sent.getValue();
    }

    private static double across(Location from, Location to) {
        return Math.hypot(from.getX() - to.getX(), from.getZ() - to.getZ());
    }

    private void hasPath(boolean path) {
        when(fixture.pathfinder.getCurrentPath()).thenReturn(path ? mock(Pathfinder.PathResult.class) : null);
    }

    @Test
    void ac11_runsWhileTheMobHasNoTarget() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock());
        assertTrue(wander.canStart());

        mob.setCurrentTarget(fixture.living(fixture.at(3, 64, 0)));

        assertFalse(wander.canStart());
    }

    @Test
    void ac11_walksAtEightTenthsSpeedToAPointWithinEightBlocksOfHomeAtHomesHeight() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock());

        wander.tick();

        final Location point = lastTrip();
        verify(fixture.pathfinder).moveTo(point, 0.8);
        assertTrue(across(point, mob.getHomeAnchor()) <= 8.0);
        assertEquals(64.0, point.getY());
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac11_picksANewPointAtMostOnceEveryThreeSeconds() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock());
        hasPath(false);

        wander.tick();
        wander.tick();
        fixture.advance(2999);
        wander.tick();
        verify(fixture.pathfinder, times(1)).moveTo(any(Location.class), anyDouble());

        fixture.advance(1);
        wander.tick();
        verify(fixture.pathfinder, times(2)).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac11_doesNotPickWhilePathing() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock());
        hasPath(true);

        wander.tick();
        fixture.advance(10_000);
        wander.tick();

        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac11_playsIdleOnceEachTimeItArrives() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock());
        hasPath(false);
        wander.tick();
        hasPath(true);
        wander.tick();

        hasPath(false);
        wander.tick();
        wander.tick();
        wander.tick();

        verify(fixture.handler, times(1)).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac11_theNumbersCanBeChanged() {
        final TestMob mob = plain();
        final WanderComponent wander = new WanderComponent(mob, fixture.clock())
                .wanderRadius(2).wanderSpeed(1.5).repathCooldownMillis(100);
        hasPath(false);

        wander.tick();
        final Location point = lastTrip();
        fixture.advance(100);
        wander.tick();

        verify(fixture.pathfinder).moveTo(point, 1.5);
        assertTrue(across(point, mob.getHomeAnchor()) <= 2.0);
        verify(fixture.pathfinder, times(2)).moveTo(any(Location.class), eq(1.5));
    }
}
