package me.mykindos.betterpvp.core.world.mapper;

import dev.brauw.mapper.region.Region;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * One world's Mapper regions, grouped by data-point name and bound to that world.
 * <p>
 * Content used to be handed a flat {@code Collection<Region>} and scan it once per data-point, which costs a full pass
 * per lookup - and every piece of content on a world repeats those passes. Grouping once at load time turns each lookup
 * into a hash hit, which is what makes running the same content across many worlds affordable.
 * <p>
 * Every region handed out has had {@link Region#setWorld(World)} applied, so {@code getLocation()} is immediately
 * usable. Forgetting that call used to produce regions pointing at the wrong world - a mistake that is invisible on a
 * single-world server and breaks the moment the same content runs on a second one.
 */
public final class RegionIndex {

    private final World world;
    private final Collection<Region> regions;
    private final Map<String, List<Region>> byName;

    private RegionIndex(@NotNull World world, @NotNull Collection<Region> regions, @NotNull Map<String, List<Region>> byName) {
        this.world = world;
        this.regions = regions;
        this.byName = byName;
    }

    public static @NotNull RegionIndex of(@NotNull World world, @NotNull Collection<? extends Region> regions) {
        final Map<String, List<Region>> byName = new HashMap<>();
        final List<Region> all = new ArrayList<>(regions.size());
        for (Region region : regions) {
            if (region == null || region.getName() == null) {
                continue;
            }
            all.add(region);
            byName.computeIfAbsent(region.getName().toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(region);
        }
        return new RegionIndex(world, Collections.unmodifiableList(all), byName);
    }

    public @NotNull World getWorld() {
        return world;
    }

    /** Every region in this world, in file order. */
    public @NotNull Collection<Region> all() {
        return regions;
    }

    /**
     * @param name the data-point name (case-insensitive)
     * @param type the region type the data-point must be authored as; others are dropped
     * @return every matching region, bound to this world
     */
    public <T extends Region> @NotNull List<T> find(@NotNull String name, @NotNull Class<T> type) {
        final List<Region> matches = byName.get(name.toLowerCase(Locale.ROOT));
        if (matches == null) {
            return List.of();
        }

        final List<T> typed = new ArrayList<>(matches.size());
        for (Region region : matches) {
            if (type.isInstance(region)) {
                region.setWorld(world);
                typed.add(type.cast(region));
            }
        }
        return typed;
    }

    /** @see #find(String, Class) */
    public <T extends Region> @NotNull Optional<T> findOne(@NotNull String name, @NotNull Class<T> type) {
        final List<T> matches = find(name, type);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    /**
     * Indexes a data-point by its {@code id:} tag, which is how map data cross-references itself (a resident naming its
     * route, a display naming its resident). Untagged regions are left out; the first holder of a duplicate id wins,
     * matching what the validators flag as an error.
     *
     * @return id (lower-cased) to region, in file order
     */
    public <T extends Region> @NotNull Map<String, T> byId(@NotNull String name, @NotNull Class<T> type) {
        final Map<String, T> byId = new LinkedHashMap<>();
        for (T region : find(name, type)) {
            final String id = RegionTags.of(region).getString("id", "").trim();
            if (!id.isEmpty()) {
                byId.putIfAbsent(id.toLowerCase(Locale.ROOT), region);
            }
        }
        return byId;
    }
}
