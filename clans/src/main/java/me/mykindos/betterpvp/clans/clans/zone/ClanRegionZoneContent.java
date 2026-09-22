package me.mykindos.betterpvp.clans.clans.zone;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.brauw.mapper.region.CuboidRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.world.content.WorldContent;
import me.mykindos.betterpvp.core.world.content.WorldContentBinding;
import me.mykindos.betterpvp.core.world.content.WorldContentService;
import me.mykindos.betterpvp.core.world.content.WorldSelector;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.NoBuildRule;
import me.mykindos.betterpvp.core.world.zone.RegionBounds;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneGameMode;
import me.mykindos.betterpvp.core.world.zone.ZoneRuleContainer;
import me.mykindos.betterpvp.core.world.zone.Zones;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * An entry may also set {@code gamemode}, which is how an area declares the mode it is played in (a training mine
 * inside a protected spawn is {@code survival}); leave it out and the zone has no opinion.
 * Files are re-read whenever the world they target loads and on a module reload.
 */
@CustomLog
@Singleton
@PluginAdapter("Mapper")
public class ClanRegionZoneContent implements WorldContent {

    private final Clans clans;
    private final ClientManager clientManager;

    @Inject
    public ClanRegionZoneContent(Clans clans, ClientManager clientManager, WorldContentService contentService) {
        this.clans = clans;
        this.clientManager = clientManager;
        contentService.register(clans, new WorldContentBinding(WorldSelector.any(), () -> List.of(this)));
    }

    @Override
    public @NotNull List<Zone> zones(@NotNull World world, @NotNull RegionIndex regions) {
        final File zonesFolder = new File(clans.getDataFolder(), "zones");
        final File[] files = zonesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return List.of();
        }

        final List<Zone> zones = new ArrayList<>();
        for (File file : files) {
            final ExtendedYamlConfiguration config = ExtendedYamlConfiguration.loadConfiguration(file);
            if (config.getString("world", BPvPWorld.MAIN_WORLD_NAME).equals(world.getName())) {
                loadFile(config, regions, zones);
            }
        }
        return zones;
    }

    /**
     * An area declaring the mode it is played in. Left unset the zone abstains and whatever it sits on decides, which
     * is what a plain safe or no-build region wants; set, it wins over that ground for everyone inside - a mine inside
     * a no-build spawn is survival because it says so.
     *
     * @param value the configured mode name, or null if the entry does not set one
     * @param name  the zone name, for logging
     * @return the parsed game mode, or empty if unset or unrecognised
     */
    private Optional<GameMode> gameMode(@Nullable String value, @NotNull String name) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(GameMode.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            log.warn("Clan zone '{}' has unknown gamemode '{}' - ignoring", name, value).submit();
            return Optional.empty();
        }
    }

    /**
     * Loads every zone defined in a single continent file. Each top-level key is a Mapper region name. The reserved
     * {@code world} key (optional, defaults to the main world) selects the world the regions live in.
     */
    private void loadFile(@NotNull ExtendedYamlConfiguration config, @NotNull RegionIndex regions,
                          @NotNull List<Zone> zones) {
        for (String name : config.getKeys(false)) {
            if (name.equalsIgnoreCase("world") || !config.isConfigurationSection(name)) {
                continue;
            }

            final List<String> tags = config.getStringList(name + ".tags");
            final int priority = config.getInt(name + ".priority", ClanZones.SERVER_REGION_PRIORITY);
            final String display = config.getString(name + ".display", name);

            final Optional<CuboidRegion> region = regions.findOne(name, CuboidRegion.class);
            if (region.isEmpty()) {
                log.warn("Clan zone '{}' has no matching Mapper region in '{}' - skipping", name,
                        regions.getWorld().getName()).submit();
                continue;
            }

            final ZoneRuleContainer rules = new ZoneRuleContainer();
            if (tags.contains(Zones.NO_BUILD)) {
                rules.add(new NoBuildRule(clientManager));
            }

            final Zone.ZoneBuilder builder = Zone.builder()
                    .key(ClanZones.regionKey(name))
                    .displayName(Component.text(display))
                    .bounds(RegionBounds.of(region.get()))
                    .priority(priority)
                    .rules(rules);
            tags.forEach(builder::tag);
            gameMode(config.getString(name + ".gamemode"), name).ifPresent(mode -> builder.gameMode(ZoneGameMode.of(mode)));

            zones.add(builder.build());
        }
    }
}
