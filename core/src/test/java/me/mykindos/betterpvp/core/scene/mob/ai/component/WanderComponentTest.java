package me.mykindos.betterpvp.core.scene.mob.ai.component;

import com.destroystokyo.paper.entity.Pathfinder;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WanderComponentTest {

    private final MobFixture fixture = new MobFixture();
    private final List<int[]> blockQueries = new ArrayList<>();
    private int floorY = Integer.MIN_VALUE;

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob mob(WanderSetup setup) {
        return fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
            created.getAi().add(setup.wander(created));
        });
    }

    private TestMob plain() {
        return fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
        });
    }

    private interface WanderSetup {
        WanderComponent wander(TestMob mob);
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

    /** Blocks are solid at {@link #floorY} and air everywhere else. */
    private void stubBlocks() {
        when(fixture.world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
        when(fixture.world.getBlockAt(any(Location.class))).thenAnswer(invocation -> {
            final Location at = invocation.getArgument(0);
            return block(at.getBlockX(), at.getBlockY(), at.getBlockZ());
        });
    }

    private Block block(int x, int y, int z) {
        blockQueries.add(new int[]{x, y, z});
        final boolean solid = y == floorY;
        final Block block = mock(Block.class);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        when(block.getWorld()).thenReturn(fixture.world);
        when(block.getLocation()).thenReturn(new Location(fixture.world, x, y, z));
        when(block.getType()).thenReturn(solid ? Material.STONE : Material.AIR);
        when(block.isSolid()).thenReturn(solid);
        when(block.isPassable()).thenReturn(!solid);
        when(block.isEmpty()).thenReturn(!solid);
        when(block.getRelative(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(x + invocation.<Integer>getArgument(0), y + invocation.<Integer>getArgument(1), z + invocation.<Integer>getArgument(2)));
        when(block.getRelative(any(BlockFace.class))).thenAnswer(invocation -> {
            final BlockFace face = invocation.getArgument(0);
            return block(x + face.getModX(), y + face.getModY(), z + face.getModZ());
        });
        return block;
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

    @Test
    void ac30_withFloorCheckingPicksASpotOnASolidBlockWithTwoFreeAbove() {
        floorY = 61;
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()).rest(1000, 2000).checkFloor(3));
        fixture.watcher();

        mob.tick();

        final Location point = lastTrip();
        assertEquals(62.0, point.getY(), 0.01);
        final double distance = across(point, mob.getHomeAnchor());
        assertTrue(distance >= 3 - 0.75 && distance <= 8 + 0.75, "picked " + distance + " from home");
    }

    @Test
    void ac30_searchesFromThreeAboveHomeToSixBelowWithUpToEightTries() {
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()).rest(1000, 2000).checkFloor(3));
        fixture.watcher();

        mob.tick();

        final Set<String> columns = new HashSet<>();
        for (int[] query : blockQueries) {
            assertTrue(query[1] >= 58 - 2 && query[1] <= 67 + 2, "looked at y " + query[1]);
            columns.add(query[0] + "," + query[2]);
        }
        assertFalse(blockQueries.isEmpty());
        assertTrue(columns.size() <= 8, "tried " + columns.size() + " columns");
        final Set<Integer> floors = new HashSet<>();
        blockQueries.forEach(query -> floors.add(query[1]));
        assertTrue(floors.contains(67) && floors.contains(58), "searched " + floors);
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac30_findingNoSpotRestsAndHoldsIdle() {
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()).rest(1000, 2000).checkFloor(3));
        fixture.watcher();
        mob.tick();
        final int searched = blockQueries.size();
        clearInvocations(fixture.handler);

        fixture.advance(999);
        tick(mob, 5);

        assertEquals(searched, blockQueries.size(), "resting, so no new search");
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        verify(fixture.handler, never()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(1002);
        tick(mob, 2);
        assertTrue(blockQueries.size() > searched, "searched again after the rest");
    }

    @Test
    void ac30_arrivingRestsForARandomTimeThenWandersAgain() {
        floorY = 61;
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()).rest(1000, 2000).checkFloor(3));
        hasPath(true);
        fixture.watcher();
        mob.tick();
        final Location point = lastTrip();

        fixture.moveBodyTo(point);
        mob.tick();
        clearInvocations(fixture.handler);
        fixture.advance(999);
        tick(mob, 5);

        verify(fixture.pathfinder, times(1)).moveTo(any(Location.class), anyDouble());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
        verify(fixture.handler, never()).playAnimation(eq("walk"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(1002);
        tick(mob, 2);
        verify(fixture.pathfinder, times(2)).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac30_givingUpRestsThenWandersAgain() {
        floorY = 61;
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()).rest(1000, 2000).checkFloor(3));
        hasPath(false);
        fixture.watcher();

        tick(mob, 400);
        verify(fixture.pathfinder, times(20)).moveTo(any(Location.class), anyDouble());
        clearInvocations(fixture.handler);
        tick(mob, 2);
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(2001);
        tick(mob, 2);
        verify(fixture.pathfinder, times(21)).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac30_withoutTheSettingsItWandersAtHomesHeightAsBefore() {
        floorY = 61;
        stubBlocks();
        final TestMob mob = mob(created -> new WanderComponent(created, fixture.clock()));
        fixture.watcher();

        mob.tick();

        final Location point = lastTrip();
        assertEquals(64.0, point.getY());
        verify(fixture.pathfinder).moveTo(point, 0.8);
    }
}
