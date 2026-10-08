package me.mykindos.betterpvp.core.world.settler.presence;

import com.destroystokyo.paper.entity.Pathfinder;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import me.mykindos.betterpvp.core.scene.mob.listener.MobCombatListener;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAssignedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerJoinedEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerLeftEvent;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.wage.SettlerStrikeEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlerPresenceTest {

    private static final String MODEL = SettlerFixture.MODEL;

    private final SettlerFixture settlers = new SettlerFixture();
    private final MobFixture fixture = settlers.fixture;
    private final MockedStatic<TagBehavior> tags = settlers.tags;
    private final MockedStatic<BoneTagAnchor> boneTags = settlers.boneTags;
    private final MockedStatic<ModelEngineHelper> helper = settlers.helper;
    private final List<List<?>> particles = settlers.particles;
    private final SettlerSite site = settlers.site;
    private final Roster roster = settlers.roster;
    private final SiteKey key = settlers.key;
    private final List<SceneSpawn> spawns = settlers.spawns;
    private final Map<String, Location> workplaces = settlers.workplaces;
    private final SettlerPresence presence = settlers.presence;

    @AfterEach
    void close() {
        settlers.close();
    }

    private Settler settler(SettlerRarity rarity, String profession, SettlerState state, String assignment) {
        return settlers.settler(rarity, profession, state, assignment);
    }

    private Settler idle() {
        return settlers.idle();
    }

    private Settler working(String workplace, Location at) {
        return settlers.working(workplace, at);
    }

    private void install() {
        settlers.install();
    }

    private void modelInstalled() {
        settlers.modelInstalled();
    }

    private SettlerNPC npc(Settler settler) {
        return settlers.npc(settler);
    }

    private SceneSpawn spawnOf(Settler settler) {
        return settlers.spawnOf(settler);
    }

    private Mob materialize(Settler settler) {
        return settlers.materialize(settler);
    }

    private static Pathfinder pathfinder(Mob body) {
        return body.getPathfinder();
    }

    private static void tickNpc(SettlerNPC npc, int times) {
        for (int i = 0; i < times; i++) {
            npc.tick();
        }
    }

    private static double horizontal(Location a, Location b) {
        final double dx = a.getX() - b.getX();
        final double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Test
    void ac1_everySettlerOnTheRosterGetsOneBodyAtTheSiteHome() {
        final Settler first = idle();
        final Settler second = idle();

        install();

        assertEquals(2, spawns.size());
        assertEquals(fixture.position(), spawnOf(first).getAnchor());
        assertEquals(fixture.position(), spawnOf(second).getAnchor());
    }

    @Test
    void ac1_withoutAHomeBodiesStandAtTheWorldSpawn() {
        when(site.home(eq(key), any(World.class), any(RegionIndex.class))).thenReturn(Optional.empty());
        final Settler settler = idle();

        install();

        assertEquals(fixture.at(100, 70, 100), spawnOf(settler).getAnchor());
    }

    @Test
    void ac1_aSettlerWhoJoinsGetsABodyAtOnce() {
        install();
        final Settler joined = idle();

        presence.onJoined(new SettlerJoinedEvent(key, joined));

        assertEquals(1, spawns.size());
        assertEquals(joined.getId(), npc(joined).getSettlerId());
    }

    @Test
    void ac1_aSettlerWhoLeavesLosesItsBodyAtOnce() {
        final Settler settler = idle();
        install();
        final Mob body = materialize(settler);
        roster.getSettlers().remove(settler);

        presence.onLeft(new SettlerLeftEvent(key, settler, SettlerLeaveReason.DISMISSED));

        assertFalse(npc(settler).isMaterialized());
        verify(body).remove();
    }

    @Test
    void ac1_theCheckAddsAndRemovesBodiesForChangesWithoutEvents() {
        final Settler gone = idle();
        install();
        final Mob body = materialize(gone);
        roster.getSettlers().remove(gone);
        final Settler added = idle();

        presence.tick();

        assertEquals(2, spawns.size());
        assertEquals(added.getId(), npc(added).getSettlerId());
        assertFalse(npc(gone).isMaterialized());
        verify(body).remove();
    }

    @Test
    void ac2_aBodyIsASceneMobRespawnedAndRedressedOnEachChunkLoad() {
        final Settler settler = working("farm", fixture.at(10, 64, 0));
        install();
        assertInstanceOf(SceneMob.class, npc(settler));
        fixture.watcher();

        final Mob first = materialize(settler);
        tickNpc(npc(settler), 2);
        verify(pathfinder(first)).moveTo(fixture.at(10, 64, 0), 0.6);

        npc(settler).dematerialize();
        verify(pathfinder(first), atLeastOnce()).stopPathfinding();

        final Mob second = materialize(settler);
        tickNpc(npc(settler), 2);
        verify(pathfinder(second)).moveTo(fixture.at(10, 64, 0), 0.6);
        tags.verify(() -> TagBehavior.addNameplate(eq(npc(settler)), any(Component.class), any(Component.class)), times(2));
    }

    @Test
    void ac3_theBodyIsTheFactorysInvulnerableBackingEntity() {
        final Settler settler = idle();
        install();
        final Pig pig = mock(Pig.class);
        when(fixture.world.spawn(any(Location.class), eq(Pig.class), any(Consumer.class))).thenAnswer(invocation -> {
            invocation.<Consumer<Pig>>getArgument(2).accept(pig);
            return pig;
        });

        final Entity entity = spawnOf(settler).getEntityFactory().apply(fixture.position());

        assertEquals(pig, entity);
        verify(pig).setInvulnerable(true);
        verify(pig).setInvisible(true);
        verify(pig).setSilent(true);
        verify(pig).setPersistent(false);
    }

    @Test
    void ac3_itsModelNeverFlashesHurt() throws ReflectiveOperationException {
        modelInstalled();
        final Settler settler = idle();
        install();
        final Mob body = materialize(settler);
        final Constructor<MobCombatListener> constructor = MobCombatListener.class.getDeclaredConstructor(SceneObjectRegistry.class);
        constructor.setAccessible(true);
        final MobCombatListener combat = constructor.newInstance(settlers.registry);
        final Player attacker = fixture.player(fixture.position().add(1, 0, 0));
        final DamageEvent hit = mock(DamageEvent.class);
        when(hit.getDamagee()).thenReturn(body);
        when(hit.getDamager()).thenReturn(attacker);
        when(hit.getDamage()).thenReturn(4.0);

        combat.onDamage(hit);

        final InOrder order = inOrder(fixture.model, fixture.modeled);
        order.verify(fixture.model).setCanHurt(false);
        order.verify(fixture.modeled).markHurt();
        verify(fixture.model, never()).setCanHurt(true);
    }

    @Test
    void ac4_wearsTheLooksModelAtItsSizeWithAHitboxScaleOf1_5() {
        modelInstalled();
        final Settler settler = idle();
        install();

        materialize(settler);

        fixture.modelEngine.verify(() -> ModelEngineAPI.createActiveModel(MODEL));
        verify(fixture.modeled).addModel(fixture.model, true);
        verify(fixture.model).setScale(1.2);
        verify(fixture.model).setHitboxScale(1.5);
    }

    @Test
    void ac4_remapsTheModelWithTheSkinWhenItIsInstalled() {
        modelInstalled();
        final ModelBlueprint skin = mock(ModelBlueprint.class);
        fixture.modelEngine.when(() -> ModelEngineAPI.getBlueprint("skin_rare")).thenReturn(skin);
        helper.when(() -> ModelEngineHelper.remapModel(any(), any())).thenAnswer(invocation -> null);
        settlers.look = new SettlerLook(MODEL, "skin_rare", "idle", "walk", "work", 1.0);
        final Settler settler = idle();
        install();

        materialize(settler);

        helper.verify(() -> ModelEngineHelper.remapModel(fixture.model, skin));
    }

    @Test
    void ac4_aSkinThatIsNotInstalledIsSkipped() {
        modelInstalled();
        helper.when(() -> ModelEngineHelper.remapModel(any(), any())).thenAnswer(invocation -> null);
        settlers.look = new SettlerLook(MODEL, "skin_missing", "idle", "walk", "work", 1.0);
        final Settler settler = idle();
        install();

        materialize(settler);

        helper.verify(() -> ModelEngineHelper.remapModel(any(), any()), never());
    }

    @Test
    void ac4_withoutTheModelInstalledTheBodyShowsNoModel() {
        final Settler settler = idle();
        install();

        materialize(settler);

        fixture.modelEngine.verify(() -> ModelEngineAPI.createActiveModel(anyString()), never());
        verify(fixture.modeled, never()).addModel(any(), anyBoolean());
    }

    @Test
    void ac5_theLooksClipsAreTheSettlersIdleWalkAndWorkClips() {
        modelInstalled();
        settlers.look = new SettlerLook(MODEL, null, "farmer_idle", "farmer_walk", "farmer_hoe", 1.0);
        final Settler settler = idle();
        install();

        final SceneMob mob = assertInstanceOf(SceneMob.class, npc(settler));
        materialize(settler);

        assertEquals("farmer_idle", mob.getAnimationProviders().get(MobAnimation.IDLE).resolve(mob));
        assertEquals("farmer_walk", mob.getAnimationProviders().get(MobAnimation.WALK).resolve(mob));
        assertEquals("farmer_hoe", mob.getAnimationProviders().get(MobAnimation.WORK).resolve(mob));
    }

    @Test
    void ac6_theNameplateFollowsTheHeadBoneWithTheProfessionAboveTheName() {
        modelInstalled();
        final Settler settler = settler(SettlerRarity.RARE, "builder", SettlerState.IDLE, null);
        install();

        materialize(settler);

        final Component name = Component.text("Aldric Tanner", NamedTextColor.BLUE);
        final Component role = Translations.component("settler.profession.builder").color(NamedTextColor.YELLOW);
        boneTags.verify(() -> BoneTagAnchor.addNameplate(npc(settler), fixture.model, "head", name, role));
    }

    @Test
    void ac6_withoutAModelTheNameplateSitsOverTheBodyAndNoProfessionShowsNothing() {
        final Settler settler = settler(SettlerRarity.LEGENDARY, null, SettlerState.IDLE, null);
        install();

        materialize(settler);

        final Component name = Component.text("Aldric Tanner", NamedTextColor.GOLD);
        tags.verify(() -> TagBehavior.addNameplate(npc(settler), name, Component.empty()));
    }

    @Test
    void ac7_aLegendarySettlerGivesOffAnEndRodParticleAboveItsHead() {
        final Settler settler = settler(SettlerRarity.LEGENDARY, null, SettlerState.IDLE, null);
        install();

        materialize(settler);

        assertEquals(1, particles.size());
        assertEquals(List.of(npc(settler), Particle.END_ROD, 1, 0.3, new Vector(0, 2.2, 0), 40), particles.get(0));
    }

    @Test
    void ac7_otherRaritiesGiveOffNoParticle() {
        settler(SettlerRarity.COMMON, null, SettlerState.IDLE, null);
        settler(SettlerRarity.UNCOMMON, null, SettlerState.IDLE, null);
        final Settler rare = settler(SettlerRarity.RARE, null, SettlerState.IDLE, null);
        install();

        for (Settler settler : List.copyOf(roster.getSettlers())) {
            materialize(settler);
        }

        assertTrue(particles.isEmpty(), "particles for " + rare.getRarity());
    }

    @Test
    void ac8_aWorkingSettlerWalksToItsWorkplaceAndHoldsItsWorkClipThere() {
        modelInstalled();
        final Location farm = fixture.at(10, 64, 0);
        final Settler settler = working("farm", farm);
        install();
        fixture.watcher();
        final Mob body = materialize(settler);

        tickNpc(npc(settler), 2);
        verify(pathfinder(body)).moveTo(farm, 0.6);

        fixture.moveBodyTo(farm);
        tickNpc(npc(settler), 2);

        verify(fixture.handler, atLeastOnce()).playAnimation(eq("work"), anyDouble(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void ac8_onlyAWorkingSettlerWithAFoundWorkplaceHasAPost() {
        final Location farm = fixture.at(10, 64, 0);
        workplaces.put("farm", farm);
        final Settler idleAssigned = settler(SettlerRarity.COMMON, "builder", SettlerState.IDLE, "farm");
        final Settler striking = settler(SettlerRarity.COMMON, "builder", SettlerState.STRIKING, "farm");
        final Settler unassigned = settler(SettlerRarity.COMMON, "builder", SettlerState.WORKING, null);
        final Settler lost = settler(SettlerRarity.COMMON, "builder", SettlerState.WORKING, "nowhere");
        install();
        fixture.watcher();

        for (Settler settler : List.of(idleAssigned, striking, unassigned, lost)) {
            final Mob body = materialize(settler);
            tickNpc(npc(settler), 5);
            verify(pathfinder(body), never()).moveTo(eq(farm), anyDouble());
        }
    }

    @Test
    void ac12_anAssignmentChangeSendsOnlyThatSettlerToItsNewPost() {
        final Location farm = fixture.at(10, 64, 0);
        final Location mill = fixture.at(-10, 64, 0);
        final Location forge = fixture.at(0.5, 64, 12);
        workplaces.put("mill", mill);
        final Settler moved = working("farm", farm);
        final Settler other = working("forge", forge);
        install();
        fixture.watcher();
        final Mob movedBody = materialize(moved);
        final Mob otherBody = materialize(other);
        tickNpc(npc(moved), 2);
        tickNpc(npc(other), 2);
        clearInvocations(pathfinder(movedBody), pathfinder(otherBody));

        moved.setAssignment("mill");
        presence.onAssigned(new SettlerAssignedEvent(key, moved, "farm"));
        tickNpc(npc(moved), 1);
        tickNpc(npc(other), 1);

        verify(pathfinder(movedBody)).moveTo(mill, 0.6);
        verify(pathfinder(otherBody), never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac12_anUnassignedSettlerGoesOffToWander() {
        final Location farm = fixture.at(10, 64, 0);
        final Settler settler = working("farm", farm);
        install();
        fixture.watcher();
        final Mob body = materialize(settler);
        tickNpc(npc(settler), 2);
        clearInvocations(pathfinder(body));

        settler.setAssignment(null);
        settler.setState(SettlerState.IDLE);
        presence.onAssigned(new SettlerAssignedEvent(key, settler, "farm"));
        tickNpc(npc(settler), 1);

        final ArgumentCaptor<Location> to = ArgumentCaptor.forClass(Location.class);
        verify(pathfinder(body)).moveTo(to.capture(), eq(0.6));
        assertFalse(to.getValue().equals(farm));
        assertTrue(horizontal(to.getValue(), fixture.position()) <= 12.75);
    }

    @Test
    void ac1_theCheckGivesABodyBackToASettlerWhoseBodyWasUnregistered() {
        final Settler settler = idle();
        install();
        final SettlerNPC first = npc(settler);
        first.remove();

        presence.tick();
        presence.tick();

        assertEquals(2, spawns.size());
        assertNotSame(first, npc(settler));
        assertTrue(npc(settler).isRegistered());
    }

    @Test
    void ac12_aSettlerWhoseStrikeEndsHeadsBackToItsPost() {
        final Location farm = fixture.at(10, 64, 0);
        workplaces.put("farm", farm);
        final Settler settler = settler(SettlerRarity.COMMON, "builder", SettlerState.STRIKING, "farm");
        install();
        fixture.watcher();
        final Mob body = materialize(settler);
        tickNpc(npc(settler), 2);
        verify(pathfinder(body), never()).moveTo(eq(farm), anyDouble());

        settler.setState(SettlerState.WORKING);
        presence.onStrike(new SettlerStrikeEvent(key, List.of(settler), false));
        tickNpc(npc(settler), 1);

        verify(pathfinder(body)).moveTo(farm, 0.6);
    }

    @Test
    void ac13_rightClickingOpensWhatTheSiteShowsForThatSettler() {
        final Settler settler = idle();
        install();
        materialize(settler);
        final Player player = fixture.player(fixture.position().add(2, 0, 0));

        npc(settler).act(player);

        verify(site).interact(player, key, settler);
    }

    @Test
    void ac13_rightClickingStopsTheSettlerAndFacesThePlayer() {
        final Settler settler = working("farm", fixture.at(10, 64, 0));
        install();
        final Mob body = materialize(settler);
        final Player player = fixture.player(fixture.position().add(2, 0, 0));
        tickNpc(npc(settler), 2);
        clearInvocations(pathfinder(body));

        npc(settler).act(player);
        tickNpc(npc(settler), 1);

        verify(pathfinder(body), atLeastOnce()).stopPathfinding();
        verify(body, atLeastOnce()).lookAt(player.getEyeLocation());
    }

    @Test
    void ac14_gatheringSendsTheSitesBodiesToAPointNearTheSpot() {
        final Settler first = working("farm", fixture.position());
        final Settler second = working("mill", fixture.position());
        install();
        fixture.watcher();
        final Mob firstBody = materialize(first);
        final Mob secondBody = materialize(second);
        tickNpc(npc(first), 2);
        tickNpc(npc(second), 2);
        clearInvocations(pathfinder(firstBody), pathfinder(secondBody));
        final Location spot = fixture.at(20.5, 64, 0.5);

        presence.gather(key, fixture.world, spot);
        tickNpc(npc(first), 1);
        tickNpc(npc(second), 1);

        for (Mob body : List.of(firstBody, secondBody)) {
            final ArgumentCaptor<Location> to = ArgumentCaptor.forClass(Location.class);
            verify(pathfinder(body)).moveTo(to.capture(), eq(0.6));
            final double distance = horizontal(to.getValue(), spot);
            assertTrue(distance >= 1 && distance <= 3, "gathered " + distance + " from the spot");
        }
    }

    @Test
    void ac14_settlersOfOtherSitesStayWhereTheyAre() {
        final Settler settler = working("farm", fixture.position());
        install();
        fixture.watcher();
        final Mob body = materialize(settler);
        tickNpc(npc(settler), 2);
        clearInvocations(pathfinder(body));

        presence.gather(SiteKey.of("camp", 8), fixture.world, fixture.at(20.5, 64, 0.5));
        tickNpc(npc(settler), 1);

        verify(pathfinder(body), never()).moveTo(any(Location.class), anyDouble());
    }

    @Test
    void ac15_aBodyThatIsNotSpawnedIgnoresGathersAndAssignmentChanges() {
        final Settler settler = working("farm", fixture.at(10, 64, 0));
        install();

        assertDoesNotThrow(() -> {
            presence.gather(key, fixture.world, fixture.at(20, 64, 0));
            presence.onAssigned(new SettlerAssignedEvent(key, settler, null));
        });
    }

    @Test
    void ac15_aBodyHeadsForItsCurrentPostWhenItsChunkLoads() {
        final Settler settler = working("farm", fixture.at(10, 64, 0));
        install();
        final Location mill = fixture.at(-10, 64, 0);
        workplaces.put("mill", mill);
        settler.setAssignment("mill");
        presence.onAssigned(new SettlerAssignedEvent(key, settler, "farm"));
        fixture.watcher();

        final Mob body = materialize(settler);
        tickNpc(npc(settler), 2);

        verify(pathfinder(body)).moveTo(mill, 0.6);
    }
}
