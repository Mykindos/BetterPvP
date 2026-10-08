package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.ai.FakeComponent;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.function.Consumer;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class AttendComponentTest {

    private final MobFixture fixture = new MobFixture();
    private final FakeComponent walking = new FakeComponent("walking", AIControl.MOVE);
    private final FakeComponent looking = new FakeComponent("looking", AIControl.LOOK);
    private AttendComponent component;
    private Player player;

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob mob(Consumer<AttendComponent> setup) {
        final TestMob mob = fixture.spawn("knight", created -> {
            created.setAnimation(MobAnimation.IDLE, "idle");
            created.setAnimation(MobAnimation.WALK, "walk");
            component = new AttendComponent(created, fixture.clock());
            setup.accept(component);
            created.getAi().add(walking);
            created.getAi().add(looking);
            created.getAi().addFirst(component);
        });
        player = fixture.player(fixture.position().add(3, 0, 0));
        mob.tick();
        return mob;
    }

    private TestMob mob() {
        return mob(attend -> { });
    }

    @Test
    void ac32_claimsMoveAndLook() {
        mob();

        assertEquals(EnumSet.of(AIControl.MOVE, AIControl.LOOK), component.getControls());
    }

    @Test
    void ac32_doesNothingUntilAsked() {
        mob();

        assertFalse(component.canStart());
        assertTrue(walking.isRunning());
        assertTrue(looking.isRunning());
    }

    @Test
    void ac32_stopsPathingFacesThePlayerAndHoldsIdle() {
        final TestMob mob = mob();

        component.attend(player);
        mob.tick();

        assertFalse(walking.isRunning());
        assertFalse(looking.isRunning());
        verify(fixture.pathfinder, atLeastOnce()).stopPathfinding();
        verify(fixture.body, atLeastOnce()).lookAt(player.getEyeLocation());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac32_afterFourSecondsWhatItInterruptedDecidesAgain() {
        final TestMob mob = mob();
        component.attend(player);
        mob.tick();

        fixture.advance(3999);
        tick(mob, 2);
        assertFalse(walking.isRunning());

        fixture.advance(2);
        tick(mob, 2);
        assertTrue(walking.isRunning());
        assertTrue(looking.isRunning());
        assertEquals(2, walking.starts);
    }

    @Test
    void ac32_askingAgainRestartsTheTime() {
        final TestMob mob = mob();
        component.attend(player);
        mob.tick();

        fixture.advance(3000);
        component.attend(player);
        mob.tick();
        fixture.advance(3000);
        tick(mob, 2);
        assertFalse(walking.isRunning());

        fixture.advance(1001);
        tick(mob, 2);
        assertTrue(walking.isRunning());
    }

    @Test
    void ac32_theTimeCanBeChanged() {
        final TestMob mob = mob(attend -> attend.durationMillis(1000));
        component.attend(player);
        mob.tick();

        fixture.advance(1001);
        tick(mob, 2);

        assertTrue(walking.isRunning());
    }
}
