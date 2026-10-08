package me.mykindos.betterpvp.core.world.settler.presence;

import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SettlerNPCTest {

    private final MobFixture fixture = new MobFixture();
    private Optional<Location> post = Optional.empty();

    @AfterEach
    void close() {
        fixture.close();
    }

    /** A settler body with a model and the look's clips, spawned at the fixture's position, which is its home. */
    private SceneMob spawn() {
        final SettlerNPC npc = new SettlerNPC(mock(SceneObjectFactory.class), UUID.randomUUID(), () -> post, fixture.clock());
        final SceneMob mob = assertInstanceOf(SceneMob.class, npc);
        mob.setModelId("settler_builder");
        mob.setAnimation(MobAnimation.IDLE, "idle");
        mob.setAnimation(MobAnimation.WALK, "walk");
        mob.setAnimation(MobAnimation.WORK, "work");
        mob.configureMaterialization(fixture.position(), anchor -> fixture.newBody());
        mob.materialize();
        return mob;
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
        final SceneMob mob = spawn();

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
        final SceneMob mob = spawn();
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
    void ac10_atItsPostItFacesTheNearestPlayerWithin5Blocks() {
        post = Optional.of(fixture.position());
        final Player near = fixture.player(fixture.position().add(3, 0, 0));
        final Player farther = fixture.player(fixture.position().add(4.5, 0, 0));
        final SceneMob mob = spawn();

        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(near.getEyeLocation());
        verify(fixture.body, never()).lookAt(farther.getEyeLocation());
    }

    @Test
    void ac10_restingItFacesAPlayerWithin5BlocksButNotOneFurther() {
        FakeGround.none(fixture.world);
        final Player outside = fixture.player(fixture.position().add(6, 0, 0));
        final SceneMob mob = spawn();
        tick(mob, 6);
        verify(fixture.body, never()).lookAt(outside.getEyeLocation());

        final Player inside = fixture.player(fixture.position().add(0, 0, 4));
        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(inside.getEyeLocation());
    }

    @Test
    void ac11_withNoPlayerWithin48BlocksItDoesNotMove() {
        post = Optional.of(fixture.at(10, 64, 0));
        fixture.player(fixture.position().add(49, 0, 0));
        final SceneMob mob = spawn();

        tick(mob, 25);

        verify(fixture.pathfinder, never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac13_aRightClickedSettlerFacesThePlayerFor4SecondsThenGoesBackToItsPost() {
        final Location farm = fixture.at(10, 64, 0);
        post = Optional.of(farm);
        final Player player = fixture.player(fixture.position().add(2, 0, 0));
        final SceneMob mob = spawn();
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

    @Test
    void ac16_settlersHaveNoRoutineOfTheirOwn() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("me.mykindos.betterpvp.core.world.settler.presence.SettlerRoutine"));
    }
}
