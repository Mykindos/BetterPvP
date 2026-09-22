package me.mykindos.betterpvp.core.world.content;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldContentServiceTest {

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<MapperHelper> mapper;
    private final List<World> worlds = new ArrayList<>();
    private WorldContentService service;

    private World main;
    private World island;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorlds).thenReturn(worlds);
        mapper = Mockito.mockStatic(MapperHelper.class);
        mapper.when(() -> MapperHelper.isBuildWorld(any())).thenReturn(false);
        mapper.when(() -> MapperHelper.readRegions(any())).thenReturn(Optional.empty());

        main = world("world");
        island = world("island_1");
        worlds.add(main);
        worlds.add(island);

        // The registry pulls packetevents in when initialised, which is not on the test classpath, and nothing here
        // spawns a scene object, so it is left out.
        service = new WorldContentService(new ZoneManager(), null);
    }

    @AfterEach
    void tearDown() {
        mapper.close();
        bukkit.close();
    }

    @Test
    void nothingIsInstalledBeforeTheServerStarts() {
        final CountingContent content = new CountingContent();
        service.register(plugin(), binding(WorldSelector.any(), content));
        assertEquals(0, content.installs.get());

        service.start();
        assertEquals(2, content.installs.get());
    }

    @Test
    void contentWearingModelsWaitsForThemAndIsReappliedOnEveryModelReload() {
        final CountingContent content = new CountingContent();
        service.register(plugin(), binding(WorldSelector.named("world"), content).withRequiresModels(true));
        service.start();
        assertEquals(0, content.installs.get());

        service.modelsReady();
        assertEquals(1, content.installs.get());

        service.modelsReady();
        assertEquals(2, content.installs.get());
        assertEquals(1, content.releases.get());
    }

    @Test
    void reloadingOnePluginLeavesAnotherPluginsContentStanding() {
        final BPvPPlugin first = plugin();
        final BPvPPlugin second = plugin();
        final CountingContent firstContent = new CountingContent();
        final CountingContent secondContent = new CountingContent();
        final AtomicInteger resets = new AtomicInteger();
        service.register(first, binding(WorldSelector.named("world"), firstContent)
                .withOnReload(resets::incrementAndGet));
        service.register(second, binding(WorldSelector.named("world"), secondContent));
        service.start();

        first.getReloadables().forEach(Reloadable::reload);

        assertEquals(1, resets.get());
        assertEquals(1, firstContent.releases.get());
        assertEquals(2, firstContent.installs.get());
        assertEquals(0, secondContent.releases.get());
        assertEquals(1, secondContent.installs.get());
    }

    @Test
    void anOwnerHooksIntoItsPluginReloadOnlyOnce() {
        final BPvPPlugin owner = plugin();
        service.register(owner, binding(WorldSelector.any(), new CountingContent()));
        service.register(owner, binding(WorldSelector.any(), new CountingContent()));

        assertEquals(1, owner.getReloadables().size());
    }

    @Test
    void unloadingAWorldReleasesOnlyThatWorld() {
        final CountingContent content = new CountingContent();
        service.register(plugin(), binding(WorldSelector.any(), content));
        service.start();

        service.unloadWorld(island);

        assertEquals(1, content.releases.get());
        assertEquals(List.of("island_1"), content.releasedWorlds);
    }

    @Test
    void releasingAPluginRemovesItsContentAndStopsItComingBack() {
        final BPvPPlugin owner = plugin();
        final CountingContent content = new CountingContent();
        service.register(owner, binding(WorldSelector.any(), content));
        service.start();

        service.release(owner);
        service.loadWorld(main);

        assertEquals(2, content.releases.get());
        assertEquals(2, content.installs.get());
    }

    @Test
    void contributorsRunOncePerWorldLoadHoweverManyBindingsMatch() {
        final AtomicInteger contributions = new AtomicInteger();
        service.registerRegions((world, authored) -> {
            contributions.incrementAndGet();
            return List.of();
        });
        final BPvPPlugin owner = plugin();
        service.register(owner, binding(WorldSelector.named("world"), new CountingContent()));
        service.register(owner, binding(WorldSelector.named("world"), new CountingContent()));
        service.start();
        assertEquals(1, contributions.get());

        service.loadWorld(main);
        assertEquals(2, contributions.get());
    }

    @Test
    void aBuildWorldGetsNoContent() {
        mapper.when(() -> MapperHelper.isBuildWorld(island)).thenReturn(true);
        final CountingContent content = new CountingContent();
        service.register(plugin(), binding(WorldSelector.any(), content));
        service.start();

        assertEquals(1, content.installs.get());
    }

    private static @NotNull WorldContentBinding binding(@NotNull WorldSelector selector, @NotNull WorldContent content) {
        return new WorldContentBinding(selector, () -> List.of(content));
    }

    private static @NotNull World world(@NotNull String name) {
        final World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        return world;
    }

    private static @NotNull BPvPPlugin plugin() {
        final BPvPPlugin plugin = mock(BPvPPlugin.class);
        final ArrayList<Reloadable> reloadables = new ArrayList<>();
        when(plugin.getReloadables()).thenReturn(reloadables);
        when(plugin.getName()).thenReturn("Test");
        return plugin;
    }

    private static final class CountingContent implements WorldContent {

        private final AtomicInteger installs = new AtomicInteger();
        private final AtomicInteger releases = new AtomicInteger();
        private final List<String> releasedWorlds = new ArrayList<>();

        @Override
        public void install(@NotNull World world, @NotNull RegionIndex regions, @NotNull WorldContentScope scope) {
            installs.incrementAndGet();
            scope.onRelease(() -> {
                releases.incrementAndGet();
                releasedWorlds.add(world.getName());
            });
        }
    }
}
