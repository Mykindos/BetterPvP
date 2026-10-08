package me.mykindos.betterpvp.core.scene.mob.animation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnimationControllerTest {

    private final MobFixture fixture = new MobFixture();

    @AfterEach
    void close() {
        fixture.close();
    }

    private TestMob modelled() {
        final TestMob mob = fixture.spawn("knight", created -> { });
        mob.setAnimation(MobAnimation.IDLE, "idle");
        mob.setAnimation(MobAnimation.WALK, "walk");
        mob.setAnimation(MobAnimation.HURT, "hurt");
        return mob;
    }

    /** Records the fresh-entry flag of every resolution. */
    private static AnimationProvider recording(List<Boolean> entries, String clip) {
        return new AnimationProvider() {
            @Override
            public String resolve(SceneMob mob) {
                return resolve(mob, true);
            }

            @Override
            public String resolve(SceneMob mob, boolean reentry) {
                entries.add(reentry);
                return clip;
            }
        };
    }

    @Test
    void ac19_aLoopingStateIsHeldAndReResolvedEveryTick() {
        final TestMob mob = modelled();
        final List<Boolean> entries = new ArrayList<>();
        mob.setAnimation(MobAnimation.IDLE, recording(entries, "idle"));
        final AnimationController animations = mob.getAnimations();

        animations.play(MobAnimation.IDLE);
        animations.tick();
        animations.tick();

        assertEquals(List.of(true, false, false), entries);
        verify(fixture.handler, times(3)).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac19_switchingStateIsAFreshEntryAndAskingAgainIsNot() {
        final TestMob mob = modelled();
        final List<Boolean> idle = new ArrayList<>();
        final List<Boolean> walk = new ArrayList<>();
        mob.setAnimation(MobAnimation.IDLE, recording(idle, "idle"));
        mob.setAnimation(MobAnimation.WALK, recording(walk, "walk"));
        final AnimationController animations = mob.getAnimations();

        animations.play(MobAnimation.IDLE);
        animations.play(MobAnimation.IDLE);
        animations.play(MobAnimation.WALK);
        animations.play(MobAnimation.IDLE);

        assertEquals(List.of(true, false, true), idle);
        assertEquals(List.of(true), walk);
    }

    @Test
    void ac19_aOneShotDoesNotReplaceTheHeldState() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        animations.play(MobAnimation.HURT);
        animations.tick();

        verify(fixture.handler, times(2)).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac20_aChangedClipStopsTheOldOneBeforeTheNewOnePlays() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        animations.play(MobAnimation.IDLE);

        final InOrder order = inOrder(fixture.handler);
        order.verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
        order.verify(fixture.handler).stopAnimation("walk");
        order.verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac20_aHeldStateThatResolvesToADifferentClipSwapsOnTick() {
        final TestMob mob = modelled();
        final AtomicBoolean combat = new AtomicBoolean();
        mob.setAnimation(MobAnimation.WALK, AnimationProviders.when(unused -> combat.get(), "walk_combat", "walk"));
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        combat.set(true);
        animations.tick();

        final InOrder order = inOrder(fixture.handler);
        order.verify(fixture.handler).stopAnimation("walk");
        order.verify(fixture.handler).playAnimation("walk_combat", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac20_anUnchangedClipIsNotStopped() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        animations.tick();
        animations.play(MobAnimation.WALK);

        verify(fixture.handler, never()).stopAnimation(anyString());
    }

    @Test
    void ac21_aOneShotReplaysOnEveryRequest() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();

        animations.play(MobAnimation.HURT);
        animations.play(MobAnimation.HURT);

        verify(fixture.handler, times(2)).playAnimation("hurt", 0.2, 0.2, 1.0, true);
    }

    @Test
    void ac21_anUnmappedAttackSendsTheVanillaSwingToEveryTracker() {
        final TestMob mob = modelled();
        final Player first = mock(Player.class);
        final Player second = mock(Player.class);
        when(fixture.body.getTrackedBy()).thenReturn(Set.of(first, second));
        final PacketEventsAPI<?> api = mock(PacketEventsAPI.class);
        final PlayerManager players = mock(PlayerManager.class);
        final User firstUser = mock(User.class);
        final User secondUser = mock(User.class);
        when(api.getPlayerManager()).thenReturn(players);
        when(players.getUser(first)).thenReturn(firstUser);
        when(players.getUser(second)).thenReturn(secondUser);
        final List<List<?>> arguments = new ArrayList<>();

        try (MockedStatic<PacketEvents> packetEvents = mockStatic(PacketEvents.class);
             MockedConstruction<WrapperPlayServerEntityStatus> packets = mockConstruction(WrapperPlayServerEntityStatus.class,
                     (packet, context) -> arguments.add(context.arguments()))) {
            packetEvents.when(PacketEvents::getAPI).thenReturn(api);

            mob.getAnimations().play(MobAnimation.ATTACK);

            assertEquals(1, packets.constructed().size());
            assertEquals(List.of(42, 4), arguments.getFirst());
            verify(firstUser).sendPacket(packets.constructed().getFirst());
            verify(secondUser).sendPacket(packets.constructed().getFirst());
        }
        verify(fixture.handler, never()).playAnimation(anyString(), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac21_otherUnmappedStatesDoNothing() {
        final TestMob mob = fixture.spawn("knight", created -> { });

        try (MockedStatic<PacketEvents> packetEvents = mockStatic(PacketEvents.class)) {
            mob.getAnimations().play(MobAnimation.HURT);
            mob.getAnimations().play(MobAnimation.DEATH);
            mob.getAnimations().play(MobAnimation.IDLE);

            packetEvents.verifyNoInteractions();
        }
        verify(fixture.handler, never()).playAnimation(anyString(), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac22_forcingALoopingStateRestartsAndHoldsIt() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();

        animations.force(MobAnimation.IDLE);
        animations.tick();

        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, true);
        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac22_forcingAOneShotRestartsItWithoutHoldingIt() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        animations.force(MobAnimation.HURT);
        animations.tick();

        verify(fixture.handler).playAnimation("hurt", 0.2, 0.2, 1.0, true);
        verify(fixture.handler, times(2)).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac22_playingAClipByNameDoesNotRestartItAndForcingDoes() {
        final TestMob mob = modelled();
        final AnimationController animations = mob.getAnimations();

        animations.play("roar");
        animations.force("roar");
        animations.play("roar", 2.0);
        animations.force("roar", 2.0);

        verify(fixture.handler).playAnimation("roar", 0.2, 0.2, 1.0, false);
        verify(fixture.handler).playAnimation("roar", 0.2, 0.2, 1.0, true);
        verify(fixture.handler).playAnimation("roar", 0.2, 0.2, 2.0, false);
        verify(fixture.handler).playAnimation("roar", 0.2, 0.2, 2.0, true);
    }

    @Test
    void ac23_withoutAModelEveryCallDoesNothing() {
        final TestMob mob = fixture.spawn(created -> { });
        mob.setAnimation(MobAnimation.IDLE, "idle");
        mob.setAnimation(MobAnimation.HURT, "hurt");
        mob.setAnimation(MobAnimation.ATTACK, "attack");
        final AnimationController animations = mob.getAnimations();

        animations.play(MobAnimation.IDLE);
        animations.tick();
        animations.play(MobAnimation.HURT);
        animations.play(MobAnimation.ATTACK);
        animations.force(MobAnimation.IDLE);
        animations.play("roar");
        animations.force("roar");

        assertFalse(animations.hasModel());
        verifyNoInteractions(fixture.handler);
    }

    @Test
    void ac23_aModelledMobReportsItsModel() {
        assertTrue(modelled().getAnimations().hasModel());
    }

    @Test
    void ac35_workIsALoopingState() {
        assertTrue(MobAnimation.WORK.isLooping());
    }

    @Test
    void ac35_workIsHeldLikeTheOtherLoopingStates() {
        final TestMob mob = modelled();
        mob.setAnimation(MobAnimation.WORK, "hammer");
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WALK);

        animations.play(MobAnimation.WORK);
        animations.tick();

        verify(fixture.handler).stopAnimation("walk");
        verify(fixture.handler, times(2)).playAnimation("hammer", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac35_idleWalkAndWorkClipsCanBeSetAfterTheMobIsBuilt() {
        final TestMob mob = modelled();
        mob.setAnimation(MobAnimation.WORK, "hammer");
        final AnimationController animations = mob.getAnimations();
        animations.play(MobAnimation.WORK);

        mob.setAnimation(MobAnimation.IDLE, "idle_settler");
        mob.setAnimation(MobAnimation.WALK, "walk_settler");
        mob.setAnimation(MobAnimation.WORK, "saw");
        animations.tick();
        animations.play(MobAnimation.WALK);
        animations.play(MobAnimation.IDLE);

        verify(fixture.handler).stopAnimation("hammer");
        verify(fixture.handler).playAnimation("saw", 0.2, 0.2, 1.0, false);
        verify(fixture.handler).playAnimation("walk_settler", 0.2, 0.2, 1.0, false);
        verify(fixture.handler).playAnimation("idle_settler", 0.2, 0.2, 1.0, false);
    }
}
