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
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Loads server-owned areas (spawn, shops, Fields, ...) as Mapper region zones. This replaces the old "admin clan"
 * model: instead of seeding a clan with {@code admin}/{@code safe} flags and claiming its chunks, an operator defines
 * a Mapper data-point and maps it to capability tags in {@code config.yml}.
 * <p>
 * Each entry under {@code zones} pairs a Mapper region name with a list of capability {@link Zones tags} and an
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
        final World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
        if (world == null) {
            log.warn("Cannot load clan region zones: world '{}' is not loaded", BPvPWorld.MAIN_WORLD_NAME).submit();
            return;
        }

        final ExtendedYamlConfiguration config = clans.getConfig();
        seedDefaults(config);
        final ConfigurationSection section = config.getConfigurationSection("zones");
        if (section == null) {
            return;
        }

        final Collection<Region> regions = MapperHelper.getRegions(world);
        int loaded = 0;
        for (String name : section.getKeys(false)) {
            final List<String> tags = section.getStringList(name + ".tags");
            final int priority = section.getInt(name + ".priority", ClanZones.SERVER_REGION_PRIORITY);
            final String display = section.getString(name + ".display", name);

            final Optional<CuboidRegion> regionOptional = MapperHelper.findRegion(regions, name, CuboidRegion.class);
            if (regionOptional.isEmpty()) {
                log.warn("Clan zone '{}' has no matching Mapper region - skipping", name).submit();
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

        log.info("Loaded {} clan region zone(s)", loaded).submit();
    }

    private void seedDefaults(ExtendedYamlConfiguration config) {
        if (config.isSet("zones")) {
            return;
        }
        config.set("zones.spawn.tags", List.of(Zones.SAFE, Zones.NO_BUILD));
        config.set("zones.spawn.priority", ClanZones.SERVER_REGION_PRIORITY);
        config.set("zones.fields.tags", List.of(ClanZones.FIELDS));
        config.set("zones.fields.priority", ClanZones.SERVER_REGION_PRIORITY - 10);
    }
}
