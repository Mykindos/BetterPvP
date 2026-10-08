package me.mykindos.betterpvp.core.world.settler.presence;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import me.mykindos.betterpvp.core.scene.behavior.AmbientParticleBehavior;
import me.mykindos.betterpvp.core.scene.behavior.BoneTagAnchor;
import me.mykindos.betterpvp.core.scene.behavior.ScriptEffectBehavior;
import me.mykindos.betterpvp.core.scene.behavior.TagBehavior;
import me.mykindos.betterpvp.core.scene.mob.MobFixture;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import me.mykindos.betterpvp.core.world.content.SceneSpawn;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A {@link SettlerPresence} on a fake camp site in a mocked world, with the fixture's clock. Bodies are registered as
 * the world's content scope does, but materialize only when a test asks. Close it after each test.
 */
final class SettlerFixture implements AutoCloseable {

    static final String WORLD = "camp_world";
    static final String MODEL = "settler_builder";

    final MobFixture fixture = new MobFixture();
    final MockedStatic<TagBehavior> tags = mockStatic(TagBehavior.class);
    final MockedStatic<BoneTagAnchor> boneTags = mockStatic(BoneTagAnchor.class);
    final MockedStatic<ModelEngineHelper> helper = mockStatic(ModelEngineHelper.class, Mockito.CALLS_REAL_METHODS);
    final MockedConstruction<ScriptEffectBehavior> scripts = mockConstruction(ScriptEffectBehavior.class);
    final List<List<?>> particles = new ArrayList<>();
    final MockedConstruction<AmbientParticleBehavior> particleBehaviors = mockConstruction(
            AmbientParticleBehavior.class, (created, context) -> particles.add(context.arguments()));

    final ProfessionRegistry professions = new ProfessionRegistry();
    final SettlerService service = new SettlerService(professions);
    final SettlerSite site = mock(SettlerSite.class);
    final SiteInstances instances = mock(SiteInstances.class);
    final SceneObjectRegistry registry = MobFixture.registry();
    final SettlerFactory factory = new SettlerFactory(registry);
    final WorldContentScope scope = mock(WorldContentScope.class);
    final RegionIndex regions = mock(RegionIndex.class);
    final Roster roster = new Roster();
    final SiteKey key = SiteKey.of("camp", 7);
    final List<SceneSpawn> spawns = new ArrayList<>();
    final Map<String, Location> workplaces = new HashMap<>();
    final SettlerPresence presence;

    SettlerLook look = new SettlerLook(MODEL, null, "idle", "walk", "work", 1.2);

    SettlerFixture() {
        professions.register(Profession.construction("builder", "settler.profession.builder", List.of()));
        service.register("camp", site);
        when(site.roster(key)).thenReturn(Optional.of(roster));
        when(site.look(eq(key), any(Settler.class))).thenAnswer(invocation -> look);
        when(site.home(eq(key), any(World.class), any(RegionIndex.class))).thenReturn(Optional.of(fixture.position()));
        when(site.workplace(eq(key), any(World.class), any(RegionIndex.class), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(workplaces.get(invocation.<String>getArgument(3))));

        final SiteInstance instance = new SiteInstance(UUID.randomUUID(), key, WORLD, SiteInstance.State.READY);
        when(instances.byWorld(WORLD)).thenReturn(Optional.of(instance));
        when(instances.forKey(key)).thenReturn(List.of(instance));
        when(fixture.world.getName()).thenReturn(WORLD);
        when(fixture.world.getSpawnLocation()).thenReturn(fixture.at(100, 70, 100));
        fixture.bukkit.when(() -> Bukkit.getWorld(WORLD)).thenReturn(fixture.world);
        FakeGround.floorAt(fixture.world, 63);

        doAnswer(invocation -> {
            final SceneSpawn spawn = invocation.getArgument(0);
            spawns.add(spawn);
            registry.register(spawn.getObject());
            return null;
        }).when(scope).add(any(SceneSpawn.class));
        presence = new SettlerPresence(service, professions, instances, factory, fixture.clock());
    }

    @Override
    public void close() {
        particleBehaviors.close();
        scripts.close();
        helper.close();
        boneTags.close();
        tags.close();
        fixture.close();
    }

    Settler settler(SettlerRarity rarity, String profession, SettlerState state, String assignment) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Aldric Tanner");
        settler.setRarity(rarity);
        settler.setProfession(profession);
        settler.setState(state);
        settler.setAssignment(assignment);
        roster.getSettlers().add(settler);
        return settler;
    }

    Settler idle() {
        return settler(SettlerRarity.COMMON, null, SettlerState.IDLE, null);
    }

    Settler working(String workplace, Location at) {
        workplaces.put(workplace, at);
        return settler(SettlerRarity.COMMON, "builder", SettlerState.WORKING, workplace);
    }

    void install() {
        presence.content().install(fixture.world, regions, scope);
    }

    void modelInstalled() {
        fixture.modelEngine.when(() -> ModelEngineAPI.getBlueprint(MODEL)).thenReturn(mock(ModelBlueprint.class));
    }

    /** The latest body given to {@code settler}. */
    SettlerNPC npc(Settler settler) {
        return (SettlerNPC) spawnOf(settler).getObject();
    }

    /** The latest spawn made for {@code settler}. */
    SceneSpawn spawnOf(Settler settler) {
        return spawns.stream()
                .filter(spawn -> ((SettlerNPC) spawn.getObject()).getSettlerId().equals(settler.getId()))
                .reduce((first, second) -> second).orElseThrow();
    }

    /** Spawns the settler's body on a fresh mocked mob at its anchor, as a chunk load would. */
    Mob materialize(Settler settler) {
        final SettlerNPC npc = npc(settler);
        npc.configureMaterialization(spawnOf(settler).getAnchor(), anchor -> fixture.newBody());
        npc.materialize();
        return (Mob) npc.getEntity();
    }
}
