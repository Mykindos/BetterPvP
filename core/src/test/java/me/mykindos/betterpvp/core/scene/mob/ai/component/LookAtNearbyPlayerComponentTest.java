package me.mykindos.betterpvp.core.scene.mob.ai.component;

import com.destroystokyo.paper.entity.Pathfinder;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.ai.FakeComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;

import java.util.EnumSet;
import java.util.function.Consumer;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LookAtNearbyPlayerComponentTest {

    private final MobFixture fixture = new MobFixture();
    private LookAtNearbyPlayerComponent component;

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob mob(Consumer<LookAtNearbyPlayerComponent> setup) {
        return fixture.spawn(created -> {
            component = new LookAtNearbyPlayerComponent(created);
            setup.accept(component);
            created.getAi().add(component);
        });
    }

    private TestMob mob() {
        return mob(look -> { });
    }

    private Location east(double blocks) {
        return fixture.position().add(blocks, 0, 0);
    }

    @Test
    void ac31_claimsOnlyLook() {
        mob();

        assertEquals(EnumSet.of(AIControl.LOOK), component.getControls());
    }

    @Test
    void ac31_facesTheNearestPlayerWithinFiveBlocks() {
        final TestMob mob = mob();
        final Player near = fixture.player(east(3));
        final Player nearer = fixture.player(fixture.position().add(0, 0, 2));

        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(nearer.getEyeLocation());
        verify(fixture.body, never()).lookAt(near.getEyeLocation());
    }

    @Test
    void ac31_ignoresPlayersBeyondItsRadius() {
        final TestMob mob = mob();
        fixture.player(east(6));

        tick(mob, 12);

        verify(fixture.body, never()).lookAt(any(Location.class));
    }

    @Test
    void ac31_theRadiusCanBeChanged() {
        final TestMob mob = mob(look -> look.radius(8));
        final Player player = fixture.player(east(7));

        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(player.getEyeLocation());
    }

    @Test
    void ac31_doesNotLookWhilePathing() {
        final TestMob mob = mob();
        fixture.player(east(3));
        when(fixture.pathfinder.getCurrentPath()).thenReturn(mock(Pathfinder.PathResult.class));

        tick(mob, 12);

        verify(fixture.body, never()).lookAt(any(Location.class));
    }

    @Test
    void ac31_looksForAPlayerEveryFiveTicks() {
        final TestMob mob = mob();
        fixture.player(east(3));
        tick(mob, 1);
        clearInvocations(fixture.world);

        tick(mob, 20);

        final long searches = mockingDetails(fixture.world).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("getNearbyPlayers"))
                .map(Invocation::getArguments)
                .filter(arguments -> arguments.length > 1 && ((Number) arguments[1]).doubleValue() == 5.0)
                .count();
        assertTrue(searches >= 3 && searches <= 5, "searched " + searches + " times in 20 ticks");
    }

    @Test
    void ac31_keepsTheSamePlayerUntilTheyLeaveTheRadius() {
        final TestMob mob = mob();
        final Player first = fixture.player(east(3));
        tick(mob, 6);
        final Player second = fixture.player(east(1));

        tick(mob, 20);
        verify(fixture.body, never()).lookAt(second.getEyeLocation());

        fixture.move(first, east(6));
        tick(mob, 6);
        verify(fixture.body, atLeastOnce()).lookAt(second.getEyeLocation());
    }

    @Test
    void ac31_dropsAPlayerWhoLeavesTheWorld() {
        final TestMob mob = mob();
        final Player first = fixture.player(east(3));
        tick(mob, 6);
        final Player second = fixture.player(east(4));

        fixture.move(first, new Location(fixture.otherWorld, 3, 64, 0));
        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(second.getEyeLocation());
    }

    @Test
    void ac31_dropsAPlayerWhoLeavesTheServer() {
        final TestMob mob = mob();
        final Player first = fixture.player(east(3));
        tick(mob, 6);
        final Player second = fixture.player(east(4));

        when(first.isOnline()).thenReturn(false);
        when(first.isValid()).thenReturn(false);
        tick(mob, 6);

        verify(fixture.body, atLeastOnce()).lookAt(second.getEyeLocation());
    }

    @Test
    void ac31_neverStopsAMovementComponent() {
        final FakeComponent walking = new FakeComponent("walking", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> {
            component = new LookAtNearbyPlayerComponent(created);
            created.getAi().add(component);
            created.getAi().add(walking);
        });
        fixture.player(east(3));

        tick(mob, 12);

        assertTrue(walking.isRunning());
        assertEquals(0, walking.stops);
    }
}
