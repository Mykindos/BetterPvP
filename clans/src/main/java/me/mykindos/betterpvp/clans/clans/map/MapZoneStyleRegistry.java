package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.data.MapFill;
import me.mykindos.betterpvp.clans.clans.map.data.MapFillStyle;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneStyle;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.zone.Zone;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The data-driven source of truth for how zones look on the map. Reads {@code clans.map.zones.<tag>.{color, alpha,
 * visible, priority, style}} into styles keyed by zone tag, so any zone carrying that tag — in any world — picks up the
 * appearance with no per-world code. Tags with no entry are hidden by default, so operators opt zones in.
 */
@CustomLog
@Singleton
public class MapZoneStyleRegistry implements Reloadable {

    private final Clans clans;
    private final MapColorBlender blender;
    private final Map<String, MapZoneStyle> stylesByTag = new ConcurrentHashMap<>();
    private final List<Runnable> invalidationListeners = new CopyOnWriteArrayList<>();
    private volatile int version;

    @Inject
    public MapZoneStyleRegistry(Clans clans, MapColorBlender blender) {
        this.clans = clans;
        this.blender = blender;
        clans.getReloadables().add(this);
        load();
    }

    @Override
    public void reload() {
        final Map<String, MapZoneStyle> previous = new ConcurrentHashMap<>(stylesByTag);
        load();
        // Only invalidate the map when the styles actually changed, so a plain /clans reload doesn't force every cached
        // pixel to re-resolve its tint (respecting the cache and keeping reload cheap).
        if (!previous.equals(stylesByTag)) {
            blender.invalidate();
            invalidateResolution();
        }
    }

    /**
     * Registers a callback fired whenever cached zone resolutions must be thrown away. The terrain cache uses this to
     * re-capture zone geometry and rebuild its mipmaps, since tints are baked into them.
     *
     * @param listener the callback, run on the main thread
     */
    public void onInvalidate(@NotNull Runnable listener) {
        invalidationListeners.add(listener);
    }

    /**
     * @return a counter bumped on every reload; cached per-pixel style resolutions compare against it to know when to
     * re-resolve after styles change.
     */
    public int version() {
        return version;
    }

    /**
     * Bumps {@link #version()} without reloading config, so callers that change which zones exist (e.g. a terrain scan)
     * force the map to re-resolve its cached per-pixel zone lookups.
     */
    public void invalidateResolution() {
        version++;
        invalidationListeners.forEach(Runnable::run);
    }

    private void load() {
        stylesByTag.clear();
        final ExtendedYamlConfiguration config = clans.getConfig();
        final ConfigurationSection root = config.getConfigurationSection("clans.map.zones");
        if (root == null) {
            log.warn("No 'clans.map.zones' config section found — map zone tints are disabled").submit();
            return;
        }

        for (String tag : root.getKeys(false)) {
            final ConfigurationSection section = root.getConfigurationSection(tag);
            if (section == null) {
                continue;
            }
            final String key = tag.toLowerCase(Locale.ROOT);
            stylesByTag.put(key, new MapZoneStyle(
                    key,
                    parseColor(section.getString("color", "#FFFFFF")),
                    section.getDouble("alpha", 0.35),
                    section.getBoolean("visible", true),
                    section.getInt("priority", 0),
                    MapFill.parse(section.getString("style"), MapFill.of(MapFillStyle.FILL)),
                    section.getBoolean("tint", true)));
        }
        log.info("Loaded {} map zone styles: {}", stylesByTag.size(), stylesByTag.keySet()).submit();
    }

    /**
     * @param tag a zone tag (case-insensitive)
     * @return the configured style for that tag, if any and visible
     */
    public @NotNull Optional<MapZoneStyle> forTag(@NotNull String tag) {
        final MapZoneStyle style = stylesByTag.get(tag.toLowerCase(Locale.ROOT));
        return style != null && style.isVisible() ? Optional.of(style) : Optional.empty();
    }

    /**
     * Picks the styled, visible tag that should win where several overlap: highest {@link MapZoneStyle#getPriority()}.
     *
     * @param zones the zones covering a location
     * @return the winning style, if any
     */
    public @NotNull Optional<MapZoneStyle> resolve(@NotNull Collection<Zone> zones) {
        MapZoneStyle best = null;
        for (Zone zone : zones) {
            for (String tag : zone.getTags()) {
                final MapZoneStyle style = stylesByTag.get(tag.toLowerCase(Locale.ROOT));
                if (style != null && style.isVisible() && style.isTintLayer()
                        && (best == null || style.getPriority() > best.getPriority())) {
                    best = style;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private Color parseColor(String raw) {
        try {
            return Color.decode(raw.startsWith("#") ? raw : "#" + raw);
        } catch (NumberFormatException exception) {
            log.warn("Invalid map zone colour '{}', defaulting to white", raw).submit();
            return Color.WHITE;
        }
    }
}
