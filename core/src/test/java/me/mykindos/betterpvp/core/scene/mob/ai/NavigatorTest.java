package me.mykindos.betterpvp.core.scene.mob.ai;

import com.destroystokyo.paper.entity.Pathfinder;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigatorTest {

    private final MobFixture fixture = new MobFixture();
    private final Navigator navigator = fixture.spawn(mob -> { }).getNavigator();
    private final Pathfinder pathfinder = fixture.pathfinder;
    private final AtomicInteger giveUps = new AtomicInteger();

    @AfterEach
    void close() {
        fixture.close();
    }

    private void tick(int times) {
        for (int i = 0; i < times; i++) {
            navigator.tick();
        }
    }

    private void hasPath(boolean path) {
        when(pathfinder.getCurrentPath()).thenReturn(path ? mock(Pathfinder.PathResult.class) : null);
    }

    @Test
    void ac5_movesToAPoint() {
        final Location point = fixture.at(10, 64, 3);

        navigator.moveTo(point, 1.2);

        verify(pathfinder).moveTo(point, 1.2);
    }

    @Test
    void ac5_movesToAnEntity() {
        final LivingEntity target = fixture.living(fixture.at(5, 64, 5));

        navigator.moveTo(target, 0.7);

        verify(pathfinder).moveTo(fixture.at(5, 64, 5), 0.7);
    }

    @Test
    void ac5_stopsPathing() {
        navigator.stop();

        verify(pathfinder).stopPathfinding();
    }

    @Test
    void ac5_reportsWhetherItHasAPath() {
        hasPath(true);
        assertTrue(navigator.isNavigating());

        hasPath(false);
        assertFalse(navigator.isNavigating());
    }

    @Test
    void ac5_doesNothingWhenTheBodyIsNotAPathfindingMob() {
        final LivingEntity plain = mock(LivingEntity.class);
        when(plain.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(plain.getUniqueId()).thenReturn(UUID.randomUUID());
        when(plain.getLocation()).thenReturn(fixture.position());
        final MobFixture.TestMob mob = fixture.create(null, created -> { });
        mob.init(plain);
        final Navigator plainNavigator = mob.getNavigator();

        plainNavigator.moveTo(fixture.at(3, 64, 3), 1.0);
        plainNavigator.moveTo(fixture.living(fixture.at(3, 64, 3)), 1.0);
        plainNavigator.stop();

        assertFalse(plainNavigator.isNavigating());
    }

    @Test
    void ac28_arrivesWithinOneAndAHalfBlocksAcrossAndTwoAndAHalfUpOrDown() {
        hasPath(true);
        navigator.travelTo(fixture.at(10, 64, 10), 1.0, giveUps::incrementAndGet);

        fixture.moveBodyTo(fixture.at(11, 66.4, 11));
        assertTrue(navigator.hasArrived());

        fixture.moveBodyTo(fixture.at(9, 61.6, 9));
        assertTrue(navigator.hasArrived());

        fixture.moveBodyTo(fixture.at(11.1, 64, 11.1));
        assertFalse(navigator.hasArrived());

        fixture.moveBodyTo(fixture.at(10, 66.6, 10));
        assertFalse(navigator.hasArrived());
    }

    @Test
    void ac28_withNoPathSearchesAgainAtMostOnceEveryTenTicks() {
        hasPath(false);
        final Location target = fixture.at(30, 64, 0);
        final AtomicInteger tick = new AtomicInteger();
        final List<Integer> searches = new ArrayList<>();
        doAnswer(invocation -> {
            searches.add(tick.get());
            return false;
        }).when(pathfinder).moveTo(any(Location.class), anyDouble());

        navigator.travelTo(target, 1.0, giveUps::incrementAndGet);
        for (int i = 1; i <= 35; i++) {
            tick.set(i);
            navigator.tick();
        }

        assertTrue(searches.size() >= 3, "searched " + searches);
        for (int i = 1; i < searches.size(); i++) {
            assertTrue(searches.get(i) - searches.get(i - 1) >= 10, "searches too close: " + searches);
        }
        verify(pathfinder, times(searches.size())).moveTo(target, 1.0);
    }

    @Test
    void ac28_aBodyThatBarelyMovesForAHundredTicksSearchesAgain() {
        hasPath(true);
        final Location target = fixture.at(30, 64, 0);
        navigator.travelTo(target, 1.0, giveUps::incrementAndGet);

        for (int i = 0; i < 90; i++) {
            fixture.moveBodyTo(fixture.position().add(0.0004, 0, 0));
            navigator.tick();
        }
        verify(pathfinder, times(1)).moveTo(target, 1.0);

        tick(20);
        verify(pathfinder, times(2)).moveTo(target, 1.0);
    }

    @Test
    void ac28_aMovingBodyDoesNotSearchAgain() {
        hasPath(true);
        final Location target = fixture.at(1000, 64, 0);
        navigator.travelTo(target, 1.0, giveUps::incrementAndGet);

        for (int i = 0; i < 300; i++) {
            fixture.moveBodyTo(fixture.position().add(0.1, 0, 0));
            navigator.tick();
        }

        verify(pathfinder, times(1)).moveTo(target, 1.0);
    }

    @Test
    void ac28_givesUpAfterTwentySearchesAndTellsTheCaller() {
        hasPath(false);
        final Location target = fixture.at(30, 64, 0);
        navigator.travelTo(target, 1.0, giveUps::incrementAndGet);

        tick(1000);

        verify(pathfinder, times(20)).moveTo(target, 1.0);
        assertEquals(1, giveUps.get());
    }

    @Test
    void ac28_anArrivedTripDoesNotGiveUp() {
        hasPath(false);
        final Location target = fixture.at(1, 64, 1);
        navigator.travelTo(target, 1.0, giveUps::incrementAndGet);

        tick(1000);

        assertTrue(navigator.hasArrived());
        assertEquals(0, giveUps.get());
    }

    @Test
    void ac28_chasingAnEntityIsUnchanged() {
        hasPath(false);
        final LivingEntity target = fixture.living(fixture.at(20, 64, 0));

        navigator.moveTo(target, 1.0);
        tick(500);

        verify(pathfinder, times(1)).moveTo(any(Location.class), anyDouble());
        verify(pathfinder, never()).stopPathfinding();
    }
}
