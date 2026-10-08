package me.mykindos.betterpvp.core.scene.mob;

import com.ticxo.modelengine.api.ModelEngineAPI;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffTestMobTest {

    private final MobFixture fixture = new MobFixture();
    private final List<List<?>> hits = new ArrayList<>();
    private final MockedConstruction<DamageEvent> damageEvents =
            mockConstruction(DamageEvent.class, (event, context) -> hits.add(context.arguments()));
    private final MockedStatic<UtilDamage> damage = mockStatic(UtilDamage.class);
    private final StaffTestMob mob = spawn();

    private StaffTestMob spawn() {
        final StaffTestMob created = new StaffTestMob(mock(SceneObjectFactory.class), fixture.clock());
        created.init(fixture.newBody());
        return created;
    }

    @AfterEach
    void close() {
        damage.close();
        damageEvents.close();
        fixture.close();
    }

    @Test
    void ac1_itIsAModelledSkeletonWarrior() {
        fixture.modelEngine.verify(() -> ModelEngineAPI.createActiveModel("skeleton_warrior"));
        verify(fixture.body).setInvisible(true);
    }

    @Test
    void ac1_withNoPlayerInRangeItWandersAroundHomeOnItsWalkClip() {
        fixture.player(fixture.at(30.5, 64, 0.5));

        mob.tick();

        assertNull(mob.getCurrentTarget());
        verify(fixture.pathfinder).moveTo(any(Location.class), eq(0.8));
        verify(fixture.handler).playAnimation("walk", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac1_whenItStopsWalkingItPlaysIdle() {
        fixture.player(fixture.at(30.5, 64, 0.5));

        mob.tick();
        mob.tick();

        verify(fixture.handler).playAnimation("idle", 0.2, 0.2, 1.0, false);
    }

    @Test
    void ac1_itTargetsTheNearestPlayerAndFacesThem() {
        fixture.player(fixture.at(10.5, 64, 0.5));
        final Player nearest = fixture.player(fixture.at(6.5, 64, 0.5));

        MobFixture.tick(mob, 2);

        assertSame(nearest, mob.getCurrentTarget());
        verify(fixture.body).lookAt(nearest.getEyeLocation());
        verify(fixture.pathfinder).moveTo(nearest.getLocation(), 1.0);
    }

    @Test
    void ac1_itLeavesPlayersInCreativeAlone() {
        final Player staff = fixture.player(fixture.at(6.5, 64, 0.5));
        when(staff.getGameMode()).thenReturn(GameMode.CREATIVE);

        MobFixture.tick(mob, 2);

        assertNull(mob.getCurrentTarget());
    }

    @Test
    void ac1_itTurnsOnWhoeverHitItOverANearerPlayer() {
        fixture.player(fixture.at(3.5, 64, 0.5));
        final Player attacker = fixture.player(fixture.at(10.5, 64, 0.5));
        mob.getThreat().add(attacker, 5);

        mob.tick();

        assertSame(attacker, mob.getCurrentTarget());
    }

    @Test
    void ac1_withinReachItSwingsItsAttackClipAndTheHitLandsAfterAWindup() {
        final Player player = fixture.player(fixture.at(2.5, 64, 0.5));

        MobFixture.tick(mob, 2);

        verify(fixture.handler).playAnimation("attack", 0.2, 0.2, 1.0, true);
        assertTrue(hits.isEmpty());

        fixture.advance(799);
        mob.tick();
        assertTrue(hits.isEmpty());

        fixture.advance(1);
        mob.tick();
        assertEquals(1, hits.size());
        assertSame(player, hits.getFirst().getFirst());
    }

    @Test
    void ac1_draggedFarFromHomeItWalksBack() {
        final Location home = fixture.position();
        fixture.moveBodyTo(fixture.at(40.5, 64, 0.5));
        fixture.player(fixture.at(70.5, 64, 0.5));

        mob.tick();

        verify(fixture.pathfinder).moveTo(home, 1.0);
    }
}
