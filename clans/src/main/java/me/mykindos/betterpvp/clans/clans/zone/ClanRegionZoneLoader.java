package me.mykindos.betterpvp.clans.clans.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import dev.brauw.mapper.region.Region;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.scene.loader.LoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.ModuleReloadLoadStrategy;
import me.mykindos.betterpvp.core.scene.loader.SceneLoaderManager;
import me.mykindos.betterpvp.core.scene.loader.ServerStartLoadStrategy;
import me.mykindos.betterpvp.core.utilities.MapperHelper;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneLoader;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Loads server-owned areas (spawn, shops, Fields, ...) as Mapper region zones. This replaces the old "admin clan"
 * model: instead of seeding a clan with {@code admin}/{@code safe} flags and claiming its chunks, an operator defines
 * a Mapper data-point and maps it to capability tags in a {@code zones/<continent>.yml} file (one file per continent,
 * scanned from the module's data folder).
 * <p>
 * Each entry in a continent file pairs a Mapper region name with a list of capability {@link Zones tags} and an
 * optional priority. Tags are composable:
 * <ul>
 *     <li>{@link Zones#SAFE} — combat is suppressed (enforced by {@code ClansCombatListener}).</li>
 *     <li>{@link Zones#NO_BUILD} — block break/place and container access are denied (enforced by a
 *     {@link NoBuildRule} attached to the zone).</li>
 *     <li>{@link ClanZones#FIELDS} — the area is a Fields resource zone (enforced by the Fields listeners).</li>
 * </ul>
 * Reuses the same {@link ServerStartLoadStrategy}/{@link ModuleReloadLoadStrategy} lifecycle as the clans scene
 * loaders, so it reloads on server start and on a module reload.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class ClanRegionZoneLoader extends ZoneLoader {

    private final Clans clans;
    private final ClientManager clientManager;

    @Inject
    public ClanRegionZoneLoader(ZoneManager zoneManager, Clans clans, ClientManager clientManager,
                                SceneLoaderManager sceneLoaderManager) {
        super(zoneManager);
        this.clans = clans;
        this.clientManager = clientManager;
        sceneLoaderManager.register(this, clans);
    }

    @Override
    public List<LoadStrategy> getStrategies() {
        return List.of(new ServerStartLoadStrategy(), new ModuleReloadLoadStrategy());
    }

    @Override
    protected void load() {
        final File zonesFolder = new File(clans.getDataFolder(), "zones");
        final File[] files = zonesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warn("No zone files found in {} - no clan region zones loaded", zonesFolder).submit();
            return;
        }

        int loaded = 0;
        for (File file : files) {
            loaded += loadFile(file);
        }
        log.info("Loaded {} clan region zone(s) from {} continent file(s)", loaded, files.length).submit();
    }

    /**
     * Loads every zone defined in a single continent file. Each top-level key is a Mapper region name; the reserved
     * {@code world} key (optional, defaults to the main world) selects the Bukkit world the regions live in.
     *
     * @return the number of zones registered from this file
     */
    private int loadFile(@NotNull File file) {
        final ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(file);
        final String worldName = config.getString("world", BPvPWorld.MAIN_WORLD_NAME);
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            log.warn("Zone file '{}' targets world '{}' which is not loaded - skipping", file.getName(), worldName).submit();
            return 0;
        }

        final Collection<Region> regions = MapperHelper.getRegions(world);
        int loaded = 0;
        for (String name : config.getKeys(false)) {
            if (name.equalsIgnoreCase("world") || !config.isConfigurationSection(name)) {
                continue;
            }

            final List<String> tags = config.getStringList(name + ".tags");
            final int priority = config.getInt(name + ".priority", ClanZones.SERVER_REGION_PRIORITY);
            final String display = config.getString(name + ".display", name);

            final Optional<CuboidRegion> regionOptional = MapperHelper.findRegion(regions, name, CuboidRegion.class);
            if (regionOptional.isEmpty()) {
                log.warn("Clan zone '{}' has no matching Mapper region in '{}' - skipping", name, worldName).submit();
                continue;
            }

            final Region region = regionOptional.get();
            region.setWorld(world);

            final ZoneRuleContainer rules = new ZoneRuleContainer();
            if (tags.contains(Zones.NO_BUILD)) {
                rules.add(new NoBuildRule(clientManager));
            }

            final Zone.ZoneBuilder builder = Zone.builder()
                    .key(ClanZones.regionKey(name))
                    .displayName(Component.text(display))
                    .bounds(RegionBounds.of(region))
                    .priority(priority)
                    .rules(rules);
            tags.forEach(builder::tag);

            register(builder.build());
            loaded++;
        }
        return loaded;
    }
}
