package me.mykindos.betterpvp.core.scene.mob;

import com.destroystokyo.paper.entity.Pathfinder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import me.mykindos.betterpvp.core.scene.SceneEntity;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.controller.SceneTicker;
import me.mykindos.betterpvp.core.scene.mob.MobFixture.TestMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.ai.FakeComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.component.PostComponent;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.mykindos.betterpvp.core.scene.mob.MobFixture.tick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SceneMobTest {

    private final MobFixture fixture = new MobFixture();

    @AfterEach
    void close() {
        fixture.close();
    }

    private static void clips(TestMob mob) {
        mob.setAnimation(MobAnimation.IDLE, "idle");
        mob.setAnimation(MobAnimation.WALK, "walk");
    }

    @Test
    void ac6_aModelledMobBindsItsModelWithTheHurtTintAndHidesItsBody() {
        final TestMob mob = fixture.spawn("knight", created -> { });

        fixture.modelEngine.verify(() -> ModelEngineAPI.createModeledEntity(eq(fixture.body), any()));
        fixture.modelEngine.verify(() -> ModelEngineAPI.createActiveModel("knight"));
        verify(fixture.modeled).addModel(fixture.model, true);
        verify(fixture.model).setCanHurt(true);
        verify(fixture.model).setDamageTint(Color.RED);
        verify(fixture.body).setInvisible(true);
        verify(fixture.body).setSilent(true);
        assertNotNull(mob.getModeledEntity());
    }

    @Test
    void ac6_noTintLeavesHurtFlashingOff() {
        final TestMob mob = fixture.create("knight", created -> { });
        mob.setDamageTint(null);

        mob.init(fixture.newBody());

        verify(fixture.model, never()).setCanHurt(anyBoolean());
        verify(fixture.model, never()).setDamageTint(any());
    }

    @Test
    void ac6_aMobWithoutAModelKeepsItsBodyVisible() {
        fixture.spawn(created -> { });

        fixture.modelEngine.verify(() -> ModelEngineAPI.createModeledEntity(any(Entity.class), any()), never());
        verify(fixture.body, never()).setInvisible(true);
        verify(fixture.body, never()).setSilent(true);
    }

    @Test
    void ac6_everyMobLosesItsGoalsAndIsHomedWhereItSpawnedThenRegistersComponents() {
        fixture.moveBodyTo(fixture.at(12, 70, -4));
        final AtomicReference<Location> homeDuringRegistration = new AtomicReference<>();
        final AtomicReference<Object> navigatorDuringRegistration = new AtomicReference<>();

        final TestMob mob = fixture.spawn(created -> {
            homeDuringRegistration.set(created.getHomeAnchor());
            navigatorDuringRegistration.set(created.getNavigator());
        });

        verify(fixture.goals).removeAllGoals(fixture.body);
        assertEquals(fixture.at(12, 70, -4), mob.getHomeAnchor());
        assertEquals(1, mob.registrations);
        assertEquals(fixture.at(12, 70, -4), homeDuringRegistration.get());
        assertNotNull(navigatorDuringRegistration.get());
    }

    @Test
    void ac7_drivesItsAIWithAPlayerInsideTheDefaultRadius() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        fixture.player(fixture.position().add(47, 0, 0));

        tick(mob, 3);

        assertEquals(48.0, mob.getActivationRadius());
        assertEquals(3, component.ticks);
    }

    @Test
    void ac7_doesNotDriveItsAIWithNoPlayerInsideTheRadius() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        fixture.player(fixture.position().add(49, 0, 0));

        tick(mob, 3);

        assertEquals(0, component.ticks);
    }

    @Test
    void ac7_theRadiusIsSetPerMob() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.create(null, created -> created.getAi().add(component));
        mob.setActivationRadius(10);
        mob.init(fixture.newBody());
        fixture.player(fixture.position().add(12, 0, 0));

        tick(mob, 3);
        assertEquals(0, component.ticks);

        fixture.player(fixture.position().add(9, 0, 0));
        tick(mob, 25);
        assertTrue(component.ticks > 0);
    }

    @Test
    void ac7_aDeadMobDoesNotDriveItsAI() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        fixture.watcher();
        when(fixture.body.isDead()).thenReturn(true);

        tick(mob, 3);

        assertEquals(0, component.ticks);
    }

    @Test
    void ac7_aMobPlayingItsDeathClipDoesNotDriveItsAI() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn("knight", created -> created.getAi().add(component));
        fixture.watcher();
        when(fixture.handler.isPlayingAnimation("death")).thenReturn(true);

        tick(mob, 3);

        assertEquals(0, component.ticks);
    }

    @Test
    void ac7_proximityIsCheckedOnceEveryTwentyTicks() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        final Player player = fixture.player(fixture.position());
        mob.tick();

        fixture.move(player, fixture.position().add(200, 0, 0));
        tick(mob, 19);
        assertEquals(20, component.ticks, "the AI kept running until the next check");

        tick(mob, 2);
        assertFalse(component.isRunning(), "the next check found nobody");
    }

    @Test
    void ac7_theAITicksBeforeTheHeldAnimationIsRefreshed() {
        final List<String> log = new ArrayList<>();
        final FakeComponent component = new FakeComponent("ai", log, AIControl.MOVE);
        final TestMob mob = fixture.spawn("knight", created -> created.getAi().add(component));
        mob.clip(MobAnimation.IDLE, created -> {
            log.add("refresh");
            return "idle";
        });
        fixture.watcher();
        mob.getAnimations().play(MobAnimation.IDLE);
        mob.tick();
        log.clear();

        mob.tick();

        assertEquals(List.of("tick ai", "refresh"), log);
    }

    @Test
    void ac7_goingInactiveStopsComponentsPathingTargetAndThreatOnce() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(component));
        final Player player = fixture.player(fixture.position());
        final LivingEntity enemy = fixture.living(fixture.at(3, 64, 0));
        mob.tick();
        mob.setCurrentTarget(enemy);
        mob.getThreat().add(enemy, 5);

        fixture.move(player, fixture.position().add(200, 0, 0));
        tick(mob, 60);

        assertEquals(1, component.stops);
        verify(fixture.pathfinder, times(1)).stopPathfinding();
        assertNull(mob.getCurrentTarget());
        assertTrue(mob.getThreat().isEmpty());
    }

    @Test
    void ac7_otherSceneBehavioursTickEitherWay() {
        final TestMob mob = fixture.spawn(created -> { });
        final AtomicInteger behaviourTicks = new AtomicInteger();
        mob.addBehavior(behaviourTicks::incrementAndGet);

        tick(mob, 5);
        assertFalse(mob.isActive());
        fixture.watcher();
        tick(mob, 25);

        assertEquals(30, behaviourTicks.get());
    }

    @Test
    void ac8_startingToMovePathsToAPointAndHoldsWalk() {
        final TestMob mob = fixture.spawn("knight", SceneMobTest::clips);
        final Location point = fixture.at(8, 64, 8);

        mob.startMoving(point, 1.3);

        verify(fixture.pathfinder).moveTo(point, 1.3);
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
        mob.getAnimations().tick();
        verify(fixture.handler, times(2)).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac8_startingToMovePathsToAnEntity() {
        final TestMob mob = fixture.spawn("knight", SceneMobTest::clips);
        final LivingEntity target = fixture.living(fixture.at(4, 64, 4));

        mob.startMoving(target, 1.0);

        verify(fixture.pathfinder).moveTo(fixture.at(4, 64, 4), 1.0);
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac8_stoppingHaltsPathingAndPlaysIdle() {
        final TestMob mob = fixture.spawn("knight", SceneMobTest::clips);
        mob.startMoving(fixture.at(8, 64, 8), 1.0);

        mob.stopMoving();

        verify(fixture.pathfinder).stopPathfinding();
        verify(fixture.handler).stopAnimation("walk");
        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac9_removingStopsComponentsClearsTargetAndThreatAndRemovesTheModel() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawn("knight", created -> created.getAi().add(component));
        final LivingEntity enemy = fixture.living(fixture.at(3, 64, 0));
        fixture.watcher();
        mob.tick();
        mob.setCurrentTarget(enemy);
        mob.getThreat().add(enemy, 4);
        final Mob body = fixture.body;

        mob.remove();

        assertEquals(1, component.stops);
        assertNull(mob.getCurrentTarget());
        assertTrue(mob.getThreat().isEmpty());
        verify(fixture.modeled).markRemoved();
        verify(body).remove();
    }

    @Test
    void ac26_aChunkManagedMobIsSpawnedAndSetUpAgainOnEachChunkLoad() {
        final TestMob mob = fixture.spawnChunkManaged("knight", created -> { });
        final Mob first = fixture.body;

        mob.dematerialize();
        mob.materialize();
        final Mob second = fixture.body;

        assertTrue(mob.isChunkManaged());
        assertEquals(2, fixture.bodies.size());
        assertTrue(first != second);
        assertEquals(second, mob.getEntity());
        fixture.modelEngine.verify(() -> ModelEngineAPI.createModeledEntity(eq(first), any()));
        fixture.modelEngine.verify(() -> ModelEngineAPI.createModeledEntity(eq(second), any()));
        verify(fixture.goals).removeAllGoals(first);
        verify(fixture.goals).removeAllGoals(second);
        verify(second).setInvisible(true);
        assertEquals(2, mob.registrations);
    }

    @Test
    void ac26_aChunkManagedMobStartsItsAIFromScratchOnEachSpawn() {
        final List<FakeComponent> registered = new ArrayList<>();
        final TestMob mob = fixture.spawnChunkManaged(null, created -> {
            final FakeComponent component = new FakeComponent("c" + registered.size(), AIControl.MOVE);
            registered.add(component);
            created.getAi().add(component);
        });
        fixture.watcher();
        final LivingEntity enemy = fixture.living(fixture.at(3, 64, 0));
        tick(mob, 2);
        mob.setCurrentTarget(enemy);
        mob.getThreat().add(enemy, 3);

        mob.dematerialize();
        mob.materialize();
        tick(mob, 3);

        assertEquals(2, registered.size());
        assertEquals(2, registered.get(0).ticks, "the first spawn's components are gone");
        assertEquals(1, registered.get(1).starts);
        assertEquals(3, registered.get(1).ticks);
        assertNull(mob.getCurrentTarget());
        assertTrue(mob.getThreat().isEmpty());
    }

    @Test
    void ac26_aMobThatIsNotChunkManagedKeepsItsOneBody() {
        final TestMob mob = fixture.spawn(created -> { });

        mob.materialize();

        assertFalse(mob.isChunkManaged());
        assertEquals(1, fixture.bodies.size());
        assertEquals(1, mob.registrations);
    }

    @Test
    void ac27_whileTheAIRunsTheBodyHasAIAndAFollowRangeOfAtLeastThePathRange() {
        final TestMob mob = fixture.create(null, created -> { });
        final Mob body = fixture.newBody();
        final AttributeInstance followRange = mock(AttributeInstance.class);
        when(followRange.getBaseValue()).thenReturn(16.0);
        when(body.getAttribute(Attribute.FOLLOW_RANGE)).thenReturn(followRange);
        mob.init(body);
        fixture.watcher();

        tick(mob, 2);

        verify(body, atLeastOnce()).setAI(true);
        verify(fixture.goals, atLeastOnce()).removeAllGoals(body);
        verify(followRange).setBaseValue(48.0);
    }

    @Test
    void ac27_aLargerFollowRangeIsKept() {
        final TestMob mob = fixture.create(null, created -> { });
        final Mob body = fixture.newBody();
        final AttributeInstance followRange = mock(AttributeInstance.class);
        when(followRange.getBaseValue()).thenReturn(64.0);
        when(body.getAttribute(Attribute.FOLLOW_RANGE)).thenReturn(followRange);
        mob.init(body);
        fixture.watcher();

        tick(mob, 2);

        verify(followRange, never()).setBaseValue(anyDouble());
    }

    @Test
    void ac27_whenTheAIStopsTheBodysAIGoesBackOffIfTheRuntimeTurnedItOn() {
        final TestMob mob = fixture.spawn(created -> { });
        final Player player = fixture.player(fixture.position());
        mob.tick();
        verify(fixture.body).setAI(true);
        when(fixture.body.hasAI()).thenReturn(true);

        fixture.move(player, fixture.position().add(200, 0, 0));
        tick(mob, 30);

        verify(fixture.body).setAI(false);
    }

    @Test
    void ac27_aBodyThatAlreadyHadAIKeepsIt() {
        final TestMob mob = fixture.create(null, created -> { });
        final Mob body = fixture.newBody();
        when(body.hasAI()).thenReturn(true);
        mob.init(body);
        final Player player = fixture.player(fixture.position());
        mob.tick();

        fixture.move(player, fixture.position().add(200, 0, 0));
        tick(mob, 30);

        verify(body, never()).setAI(false);
    }

    @Test
    void ac32_askingAMobToAttendStopsWhatItWasDoingAndFacesThePlayer() {
        final FakeComponent walking = new FakeComponent("walking", AIControl.MOVE);
        final TestMob mob = fixture.spawn("knight", created -> {
            clips(created);
            created.getAi().add(walking);
        });
        fixture.watcher();
        final Player player = fixture.player(fixture.at(3, 64, 0));
        mob.tick();

        mob.attend(player);
        mob.tick();

        assertFalse(walking.isRunning());
        verify(fixture.pathfinder, atLeastOnce()).stopPathfinding();
        verify(fixture.body, atLeastOnce()).lookAt(player.getEyeLocation());
        verify(fixture.handler, atLeastOnce()).playAnimation(eq("idle"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac33_orderingAMobToASpotSendsItNearTheSpot() {
        final FakeComponent walking = new FakeComponent("walking", AIControl.MOVE);
        final TestMob mob = fixture.spawn(created -> created.getAi().add(walking));
        fixture.watcher();
        mob.tick();
        final Location spot = fixture.at(20, 64, 20);

        mob.orderTo(spot);
        mob.tick();

        assertFalse(walking.isRunning());
        final ArgumentCaptor<Location> sent = ArgumentCaptor.forClass(Location.class);
        verify(fixture.pathfinder, atLeastOnce()).moveTo(sent.capture(), anyDouble());
        final double distance = Math.hypot(sent.getValue().getX() - 20, sent.getValue().getZ() - 20);
        assertTrue(distance >= 1 && distance <= 3, "sent " + distance + " from the spot");
    }

    @Test
    void ac34_replanningStopsRunningComponentsSoEachDecidesAgainNextTick() {
        final FakeComponent walking = new FakeComponent("walking", AIControl.MOVE);
        final FakeComponent thinking = new FakeComponent("thinking");
        final TestMob mob = fixture.spawn(created -> {
            created.getAi().add(walking);
            created.getAi().add(thinking);
        });
        fixture.watcher();
        mob.tick();

        mob.replan();

        assertEquals(1, walking.stops);
        assertEquals(1, thinking.stops);
        mob.tick();
        assertEquals(2, walking.starts);
        assertEquals(2, thinking.starts);
    }

    @Test
    void ac34_aChangedPostLocationIsPickedUpOnReplan() {
        final Location first = fixture.at(10, 64, 0);
        final Location second = fixture.at(-10, 64, 0);
        final AtomicReference<Location> post = new AtomicReference<>(first);
        final TestMob mob = fixture.spawn(created ->
                created.getAi().add(new PostComponent(created, () -> Optional.of(post.get()))));
        fixture.watcher();
        when(fixture.pathfinder.getCurrentPath()).thenReturn(mock(Pathfinder.PathResult.class));
        mob.tick();
        verify(fixture.pathfinder).moveTo(eq(first), anyDouble());

        post.set(second);
        tick(mob, 5);
        verify(fixture.pathfinder, never()).moveTo(eq(second), anyDouble());

        mob.replan();
        mob.tick();
        verify(fixture.pathfinder).moveTo(eq(second), anyDouble());
    }

    @Test
    void ac36_whenAChunkManagedMobUnloadsItsBodyGoesAndItsComponentsStop() {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawnChunkManaged(null, created -> created.getAi().add(component));
        fixture.watcher();
        tick(mob, 2);
        final Mob body = fixture.body;

        mob.dematerialize();

        verify(body).remove();
        assertFalse(mob.isMaterialized());
        assertEquals(1, component.stops, "components stop as the chunk unloads");
    }

    @Test
    void ac36_nothingAboutAnUnloadedMobTicksUntilItsChunkLoads() throws Exception {
        final FakeComponent component = new FakeComponent("c", AIControl.MOVE);
        final TestMob mob = fixture.spawnChunkManaged(null, created -> created.getAi().add(component));
        fixture.watcher();
        final AtomicInteger behaviourTicks = new AtomicInteger();
        mob.addBehavior(behaviourTicks::incrementAndGet);
        final SceneTicker ticker = ticker(mob);
        ticker.onUpdate();
        final int ticksBefore = component.ticks;

        mob.dematerialize();
        for (int i = 0; i < 40; i++) {
            ticker.onUpdate();
        }

        assertEquals(ticksBefore, component.ticks);
        assertEquals(1, behaviourTicks.get());
    }

    private static SceneTicker ticker(SceneEntity entity) throws Exception {
        final SceneObjectRegistry registry = mock(SceneObjectRegistry.class);
        when(registry.getObjects(SceneEntity.class)).thenReturn(List.of(entity));
        final Constructor<SceneTicker> constructor = SceneTicker.class.getDeclaredConstructor(SceneObjectRegistry.class);
        constructor.setAccessible(true);
        return constructor.newInstance(registry);
    }
}
