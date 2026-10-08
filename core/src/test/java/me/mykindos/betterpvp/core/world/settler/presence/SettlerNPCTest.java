package me.mykindos.betterpvp.core.world.settler.presence;

import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SettlerNPCTest {

    private static final String POST = "post";

    private final SettlerFixture settlers = new SettlerFixture();
    private final MobFixture fixture = settlers.fixture;

    @AfterEach
    void close() {
        settlers.close();
    }

    /** Puts the settler's workplace at {@code at}. */
    private void post(Location at) {
        settlers.workplaces.put(POST, at);
    }

    /**
     * A working settler with the look's model and clips, whose workplace is wherever {@link #post} put it, spawned
     * by the presence at the fixture's position, which is its home.
     */
    private SettlerNPC spawn() {
        settlers.modelInstalled();
        final Settler settler = settlers.settler(SettlerRarity.COMMON, "builder", SettlerState.WORKING, POST);
        settlers.install();
        settlers.materialize(settler);
        final SettlerNPC npc = settlers.npc(settler);
        assertNull(npc.getModelId());
        return npc;
    }

    private Location lastTrip() {
        final ArgumentCaptor<Location> to = ArgumentCaptor.forClass(Location.class);
        verify(fixture.pathfinder, atLeastOnce()).moveTo(to.capture(), eq(0.6));
        return to.getValue();
    }

    private static double horizontal(Location a, Location b) {
        final double dx = a.getX() - b.getX();
        final double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Test
    void ac9_withNoPostItWalksToAStandableSpot2To12BlocksFromHome() {
        FakeGround.floorAt(fixture.world, 63);
        final Location home = fixture.position();
        fixture.watcher();
        final SettlerNPC mob = spawn();

        for (int trip = 0; trip < 20; trip++) {
            clearInvocations(fixture.pathfinder);
            mob.replan();
            tick(mob, 1);
            final Location to = lastTrip();
            final double distance = horizontal(to, home);
            // Spots are block centres, so a rolled distance can round by up to half a block diagonal.
            assertTrue(distance >= 2 - 0.71 && distance <= 12 + 0.71, "wandered " + distance + " from home");
            assertEquals(64, to.getY());
            assertEquals(0.5, Math.abs(to.getX() % 1));
            assertEquals(0.5, Math.abs(to.getZ() % 1));
        }
    }

    @Test
    void ac9_afterArrivingItRests5To15SecondsHoldingIdleThenPicksAnotherSpot() {
        FakeGround.floorAt(fixture.world, 63);
        fixture.watcher();
        final SettlerNPC mob = spawn();
        tick(mob, 1);
        final Location first = lastTrip();

        fixture.moveBodyTo(first);
        clearInvocations(fixture.pathfinder, fixture.handler);
        tick(mob, 1);
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());

        fixture.advance(4_999);
        tick(mob, 5);
        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());

        fixture.advance(10_002);
        tick(mob, 1);
        verify(fixture.pathfinder).moveTo(any(Location.class), eq(0.6));
    }

    @Test
    void ac8_aWorkplaceFoundLateIsWalkedToAfterTheNextRest() {
        FakeGround.floorAt(fixture.world, 63);
        fixture.watcher();
        final SettlerNPC mob = spawn();
        tick(mob, 1);

        final Location farm = fixture.at(10, 64, 0);
        post(farm);
        tick(mob, 5);
        verify(fixture.pathfinder, never()).moveTo(farm, 0.6);

        fixture.advance(15_001);
        tick(mob, 1);

        verify(fixture.pathfinder).moveTo(farm, 0.6);
    }

    @Test
    void ac10_atItsPostItFacesTheNearestPlayerWithin5Blocks() {
        post(fixture.position());
        final Player near = fixture.player(fixture.position().add(3, 0, 0));
        final Player farther = fixture.player(fixture.position().add(4.5, 0, 0));
        final SettlerNPC mob = spawn();

        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(near.getEyeLocation());
        verify(fixture.body, never()).lookAt(farther.getEyeLocation());
    }

    @Test
    void ac10_restingItFacesAPlayerWithin5BlocksButNotOneFurther() {
        FakeGround.none(fixture.world);
        final Player outside = fixture.player(fixture.position().add(6, 0, 0));
        final SettlerNPC mob = spawn();
        tick(mob, 6);
        verify(fixture.body, never()).lookAt(outside.getEyeLocation());

        final Player inside = fixture.player(fixture.position().add(0, 0, 4));
        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(inside.getEyeLocation());
    }

    @Test
    void ac11_withNoPlayerWithin48BlocksItDoesNotMove() {
        post(fixture.at(10, 64, 0));
        fixture.player(fixture.position().add(49, 0, 0));
        final SettlerNPC mob = spawn();

        tick(mob, 25);

        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac13_aRightClickedSettlerFacesThePlayerFor4SecondsThenGoesBackToItsPost() {
        final Location farm = fixture.at(10, 64, 0);
        post(farm);
        final Player player = fixture.player(fixture.position().add(2, 0, 0));
        final SettlerNPC mob = spawn();
        tick(mob, 1);
        clearInvocations(fixture.pathfinder);

        mob.attend(player);
        tick(mob, 1);
        verify(fixture.body, atLeastOnce()).lookAt(player.getEyeLocation());
        fixture.advance(3_900);
        tick(mob, 1);
        verify(fixture.pathfinder, never()).moveTo(eq(farm), anyDouble());

        fixture.advance(200);
        tick(mob, 2);
        verify(fixture.pathfinder).moveTo(farm, 0.6);
    }
}
