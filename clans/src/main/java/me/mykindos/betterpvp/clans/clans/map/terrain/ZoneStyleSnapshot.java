package me.mykindos.betterpvp.clans.clans.map.terrain;

import me.mykindos.betterpvp.clans.clans.map.MapZoneStyleRegistry;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneStyle;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable, off-thread-safe view of the styled zones covering one world, captured on the main thread so the mip
 * builder can resolve tints without touching the live {@link ZoneManager}.
 * <p>
 * Only zones carrying a <em>tint-layer</em> style are captured, which is what keeps clan territory (rendered per chunk
 * from its own index) out of this path — and with it the chunk-PDC zone provider that would be unsafe to query from a
 * worker thread. The zones held here resolve through pure geometry.
 */
public class ZoneStyleSnapshot {

    private static final byte STYLE_NONE = 0;

    private final MapZoneStyle[] styles;
    private final List<Zone>[] zonesByStyle;
    private final World world;

    @SuppressWarnings("unchecked")
    private ZoneStyleSnapshot(World world, List<MapZoneStyle> styles, List<List<Zone>> zones) {
        this.styles = styles.toArray(MapZoneStyle[]::new);
        this.zonesByStyle = zones.toArray(List[]::new);
        this.world = world;
    }

    /**
     * Captures the styled zones covering a world. Must be called on the main thread.
     *
     * @param world       the world to capture
     * @param zoneManager the live zone manager
     * @param registry    the style registry deciding which tags paint
     * @return a snapshot safe to hand to a worker thread
     */
    public static @NotNull ZoneStyleSnapshot capture(@NotNull World world, @NotNull ZoneManager zoneManager,
                                                     @NotNull MapZoneStyleRegistry registry) {
        final List<MapZoneStyle> styles = new ArrayList<>();
        final List<List<Zone>> zones = new ArrayList<>();

        for (Zone zone : zoneManager.getAllZones()) {
            if (zone.getWorld() != null && !zone.getWorld().equals(world)) {
                continue;
            }
            registry.resolve(List.of(zone)).ifPresent(style -> {
                int index = styles.indexOf(style);
                if (index < 0) {
                    index = styles.size();
                    styles.add(style);
                    zones.add(new ArrayList<>());
                }
                zones.get(index).add(zone);
            });
        }
        return new ZoneStyleSnapshot(world, styles, zones);
    }

    /**
     * Resolves by column rather than by point: the map draws one pixel per column, so a zone floating above the
     * surface — a dock whose box starts at deck height and reaches over the water beside it — still paints every
     * column it stands over. {@code y} is passed on only for bounds that genuinely cannot answer without one.
     *
     * @return the winning style index + 1 for a column, or {@code 0} when no styled zone covers it
     */
    public byte resolve(int x, int y, int z) {
        int best = -1;
        int bestPriority = Integer.MIN_VALUE;
        for (int index = 0; index < zonesByStyle.length; index++) {
            final int priority = styles[index].getPriority();
            if (priority <= bestPriority) {
                continue;
            }
            for (Zone zone : zonesByStyle[index]) {
                if (zone.containsColumn(world, x, y, z)) {
                    best = index;
                    bestPriority = priority;
                    break;
                }
            }
        }
        return best < 0 ? STYLE_NONE : (byte) (best + 1);
    }

    /**
     * Whether a style's own zones reach a column, whatever wins the tint there.
     * <p>
     * This is the question an outline asks, and it is not the same as "does this style win here". A dock with a beach
     * inside it still <em>covers</em> the beach: if the border followed the winning style it would trace around every
     * higher-priority patch nested in a zone, so a bay full of moored ships came out outlined ship by ship instead of
     * once around the dock.
     *
     * @param resolved a value previously returned by {@link #resolve}
     * @return whether any zone carrying that style covers the column
     */
    public boolean covers(byte resolved, int x, int y, int z) {
        final int index = resolved - 1;
        if (index < 0 || index >= zonesByStyle.length) {
            return false;
        }
        for (Zone zone : zonesByStyle[index]) {
            if (zone.containsColumn(world, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param resolved a value previously returned by {@link #resolve}
     * @return the style it names, or {@code null} for {@code 0} / an index this snapshot no longer holds
     */
    public @Nullable MapZoneStyle style(byte resolved) {
        final int index = resolved - 1;
        return index < 0 || index >= styles.length ? null : styles[index];
    }
}
