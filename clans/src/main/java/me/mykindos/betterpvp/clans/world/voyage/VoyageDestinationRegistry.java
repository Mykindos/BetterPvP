package me.mykindos.betterpvp.clans.world.voyage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import me.mykindos.betterpvp.clans.world.travel.Destination;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The places a helm can set a course for, declared in {@code voyages.yml}.
 * <p>
 * Kept out of {@code islands.yml} because every top-level key there is read as a discovery-island template; a voyages
 * section in that file would be reported as a template missing its world folder.
 *
 * <pre>
 * aldenmark:
 *   display-name: "Aldenmark"
 *   world: "world"
 *   icon: GRASS_BLOCK
 *   min-seconds: 60
 *   max-seconds: 120
 *   chance: 0.25
 *   distribution: random        # random | round-robin | first
 * </pre>
 */
@Singleton
@CustomLog
public class VoyageDestinationRegistry implements Reloadable {

    private final Map<String, VoyageDestination> destinations = new LinkedHashMap<>();
    private final Clans clans;
    private final CrewService crewService;
    private final VoyageService voyageService;

    @Inject
    public VoyageDestinationRegistry(@NotNull Clans clans, @NotNull CrewService crewService,
                                     @NotNull VoyageService voyageService) {
        this.clans = clans;
        this.crewService = crewService;
        this.voyageService = voyageService;
    }

    /** Everything currently reachable — a destination whose world is not loaded is left out rather than offered. */
    public @NotNull List<Destination> available() {
        final List<Destination> ready = new ArrayList<>();
        for (VoyageDestination destination : destinations.values()) {
            if (destination.isReady()) {
                ready.add(destination);
            }
        }
        return ready;
    }

    @Override
    public void reload() {
        final ExtendedYamlConfiguration config = clans.getConfig("voyages");
        for (String key : config.getKeys(false)) {
            final ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                log.warn("Invalid voyage destination section: {}", key).submit();
                continue;
            }

            final String worldName = section.getString("world");
            if (worldName == null || worldName.isBlank()) {
                log.warn("Voyage destination '{}' is missing 'world' - skipping", key).submit();
                continue;
            }

            final Material icon = Material.matchMaterial(section.getString("icon", "GRASS_BLOCK"));
            final VoyageTiming timing = VoyageTiming.of(
                    section.getInt("min-seconds", VoyageTiming.DEFAULT.getMinSeconds()),
                    section.getInt("max-seconds", VoyageTiming.DEFAULT.getMaxSeconds()),
                    section.getDouble("chance", VoyageTiming.DEFAULT.getChancePerRoll()));

            destinations.put(key.toLowerCase(Locale.ROOT), new VoyageDestination(
                    Key.key("betterpvp", "voyage/" + key.toLowerCase(Locale.ROOT)),
                    Component.text(section.getString("display-name", key)),
                    ItemView.builder().material(icon != null ? icon : Material.GRASS_BLOCK).build(),
                    worldName,
                    timing,
                    ArrivalDistribution.byName(section.getString("distribution", "random")),
                    crewService,
                    voyageService));
        }

        log.info("Loaded {} voyage destination(s)", destinations.size()).submit();
    }
}
