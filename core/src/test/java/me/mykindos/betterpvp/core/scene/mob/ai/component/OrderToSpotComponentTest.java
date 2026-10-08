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
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class OrderToSpotComponentTest {

    private final MobFixture fixture = new MobFixture();
    private final Location spot = fixture.at(20, 64, 20);
    private final List<OrderToSpotComponent> components = new ArrayList<>();
    private final List<FakeComponent> lowers = new ArrayList<>();

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob mob() {
        final TestMob mob = fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
            final OrderToSpotComponent component = new OrderToSpotComponent(created, fixture.clock()).rest(1000, 2000);
            final FakeComponent lower = new FakeComponent("lower", AIControl.MOVE);
            components.add(component);
            lowers.add(lower);
            created.getAi().addFirst(component);
            created.getAi().add(lower);
        });
        when(fixture.pathfinder.getCurrentPath()).thenReturn(mock(Pathfinder.PathResult.class));
        return mob;
    }

    private static Location sentTo(Pathfinder pathfinder) {
        final ArgumentCaptor<Location> sent = ArgumentCaptor.forClass(Location.class);
        verify(pathfinder, atLeastOnce()).moveTo(sent.capture(), anyDouble());
        return sent.getValue();
    }

    private double fromSpot(Location point) {
        return Math.hypot(point.getX() - spot.getX(), point.getZ() - spot.getZ());
    }

    @Test
    void ac33_doesNothingUntilOrdered() {
        final TestMob mob = mob();
        fixture.watcher();

        tick(mob, 3);

        assertFalse(components.getFirst().canStart());
        assertTrue(lowers.getFirst().isRunning());
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac33_sendsTheMobToARandomPointOneToThreeBlocksFromTheSpot() {
        final TestMob mob = mob();
        fixture.watcher();
        mob.tick();

        components.getFirst().orderTo(spot);
        mob.tick();

        assertFalse(lowers.getFirst().isRunning());
        final Location point = sentTo(fixture.pathfinder);
        assertTrue(fromSpot(point) >= 1 && fromSpot(point) <= 3, "sent " + fromSpot(point) + " from the spot");
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac33_restsThereThenLetsWhatItWasDoingDecideAgain() {
        final TestMob mob = mob();
        fixture.watcher();
        mob.tick();
        components.getFirst().orderTo(spot);
        mob.tick();

        fixture.moveBodyTo(sentTo(fixture.pathfinder));
        mob.tick();
        clearInvocations(fixture.handler);
        fixture.advance(999);
        tick(mob, 3);
        assertFalse(lowers.getFirst().isRunning());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        verify(fixture.handler, never()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(1002);
        tick(mob, 2);

        assertTrue(lowers.getFirst().isRunning());
        assertEquals(2, lowers.getFirst().starts);
        verify(fixture.pathfinder, times(1)).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac33_severalMobsSentToOneSpotSpreadAroundIt() {
        final List<TestMob> mobs = new ArrayList<>();
        final List<Pathfinder> pathfinders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mobs.add(mob());
            pathfinders.add(fixture.pathfinder);
        }
        fixture.watcher();

        for (int i = 0; i < mobs.size(); i++) {
            mobs.get(i).tick();
            components.get(i).orderTo(spot);
            mobs.get(i).tick();
        }

        final Set<String> points = new HashSet<>();
        for (Pathfinder pathfinder : pathfinders) {
            final Location point = sentTo(pathfinder);
            assertTrue(fromSpot(point) >= 1 && fromSpot(point) <= 3);
            points.add(Math.round(point.getX() * 10) + "," + Math.round(point.getZ() * 10));
        }
        assertTrue(points.size() >= 5, "only " + points.size() + " distinct points for 10 mobs");
    }
}
