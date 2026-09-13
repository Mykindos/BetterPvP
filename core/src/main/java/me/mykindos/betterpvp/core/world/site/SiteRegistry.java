package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Every site the server knows about, read from {@code sites.yml}. Parsing takes a {@link ConfigurationSection}, so the
 * catalogue can be checked without a running server.
 */
@CustomLog
@Singleton
public class SiteRegistry implements Reloadable {

    private final Map<String, Site> sites = new LinkedHashMap<>();
    private final Core core;

    @Inject
    public SiteRegistry(@NotNull Core core) {
        this.core = core;
    }

    public @NotNull Optional<Site> get(@NotNull String id) {
        return Optional.ofNullable(sites.get(id.toLowerCase(Locale.ROOT)));
    }

    public @NotNull Collection<Site> all() {
        return sites.values();
    }

    @Override
    public void reload() {
        load(core.getConfig("sites"));
    }

    /**
     * Replaces the catalogue with everything under {@code root}. A site that fails to parse is skipped rather than
     * failing the load.
     */
    public void load(@NotNull ConfigurationSection root) {
        sites.clear();

        for (String id : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                log.warn("Invalid site section: {}", id).submit();
                continue;
            }

            final WorldSource worldSource = worldSource(id, section.getConfigurationSection("world"));
            if (worldSource == null) {
                continue;
            }

            sites.put(id.toLowerCase(Locale.ROOT), new Site(
                    id.toLowerCase(Locale.ROOT),
                    Component.text(section.getString("display-name", id)),
                    icon(section.getString("icon", "GRASS_BLOCK")),
                    worldSource,
                    policy(section),
                    timing(section.getConfigurationSection("voyage")),
                    arrivalSection(section).getString("marker", ArrivalPoints.DEFAULT_MARKER),
                    ArrivalDistribution.byName(arrivalSection(section).getString("distribution", "random"))));
        }

        log.info("Loaded {} site(s)", sites.size()).submit();
    }

    private @Nullable WorldSource worldSource(@NotNull String id, @Nullable ConfigurationSection world) {
        if (world == null) {
            log.warn("Site '{}' is missing a 'world' block - skipping", id).submit();
            return null;
        }

        final String adopt = world.getString("adopt");
        if (adopt != null) {
            return WorldSource.adopt(adopt);
        }

        final String template = world.getString("clone");
        if (template != null) {
            return WorldSource.clone(template);
        }

        final String folder = world.getString("own");
        if (folder != null) {
            return WorldSource.own(folder, world.getString("from"));
        }

        log.warn("Site '{}' has a 'world' block with no adopt, clone or own - skipping", id).submit();
        return null;
    }

    private @NotNull SitePolicy policy(@NotNull ConfigurationSection section) {
        return SitePolicy.builder()
                .lifecycle(enumValue(SitePolicy.Lifecycle.class, section.getString("lifecycle"), SitePolicy.Lifecycle.PERMANENT))
                .min(section.getInt("min", 0))
                .max(section.getInt("max", 0))
                .capacity(section.getInt("capacity", 0))
                .admission(admission(section.getString("admission", "open")))
                .selection(selection(section.getString("selection", "fill-first")))
                .rejoin(enumValue(SitePolicy.Rejoin.class, section.getString("rejoin"), SitePolicy.Rejoin.ANCHOR))
                .rejoinAt(enumValue(SitePolicy.RejoinAt.class, section.getString("rejoin-at"), SitePolicy.RejoinAt.SPAWN_POINT))
                .anchorable(section.getBoolean("anchorable", false))
                .dormancy(enumValue(SitePolicy.Dormancy.class, section.getString("dormancy"), SitePolicy.Dormancy.ALWAYS_LOADED))
                .dormancyGraceSeconds(section.getInt("dormancy-grace-seconds", 300))
                .fallbackSiteId(section.getString("fallback"))
                .server(section.getString("server"))
                .build();
    }

    private @NotNull TransitTiming timing(@Nullable ConfigurationSection voyage) {
        if (voyage == null) {
            return TransitTiming.DEFAULT;
        }

        return TransitTiming.of(
                voyage.getInt("min-seconds", TransitTiming.DEFAULT.getMinSeconds()),
                voyage.getInt("max-seconds", TransitTiming.DEFAULT.getMaxSeconds()),
                voyage.getDouble("chance", TransitTiming.DEFAULT.getChancePerRoll()));
    }

    private @NotNull ConfigurationSection arrivalSection(@NotNull ConfigurationSection section) {
        final ConfigurationSection arrival = section.getConfigurationSection("arrival");
        return arrival == null ? section.createSection("arrival") : arrival;
    }

    /**
     * Resolves an admission rule by name. A name that is not registered yet is looked up again on each use, since a
     * module registers its own rules while it starts up.
     */
    private @NotNull Admission admission(@NotNull String name) {
        final Admission known = Admission.byName(name);
        if (known != null) {
            return known;
        }

        return (key, occupants, party) -> {
            final Admission resolved = Admission.byName(name);
            if (resolved == null) {
                log.warn("Site admission rule '{}' has never been registered - refusing to share instances", name).submit();
                return false;
            }
            return resolved.admits(key, occupants, party);
        };
    }

    private @NotNull Selection selection(@NotNull String name) {
        final Selection known = Selection.byName(name);
        if (known != null) {
            return known;
        }

        log.warn("Unknown site selection rule '{}' - falling back to fill-first", name).submit();
        return Selection.fillFirst();
    }

    private @NotNull Material icon(@NotNull String name) {
        final Material material = Material.matchMaterial(name);
        return material == null ? Material.GRASS_BLOCK : material;
    }

    private <E extends Enum<E>> @NotNull E enumValue(@NotNull Class<E> type, @Nullable String name, @NotNull E fallback) {
        if (name == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown {} value '{}' - falling back to {}", type.getSimpleName(), name, fallback).submit();
            return fallback;
        }
    }
}
