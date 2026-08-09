package me.mykindos.betterpvp.clans.clans.map;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneStyle;
import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Blends a zone's translucent tint over an already-shaded terrain pixel and quantises the result back to a Minecraft
 * map colour. The blend depends only on (terrain colour, zone style), a small finite set, so results are cached — the
 * hot render path never recomputes a blend it has seen, which is what keeps an always-on tint affordable.
 */
@Singleton
@SuppressWarnings("deprecation") // MapPalette colour<->byte is the simplest correct quantiser and is stable
public class MapColorBlender {

    private final ConcurrentMap<Long, Byte> cache = new ConcurrentHashMap<>();

    /**
     * @param terrainPacked the shaded terrain pixel's packed map colour
     * @param style         the tint to apply
     * @return the packed map colour of the terrain tinted by the style
     */
    public byte blend(byte terrainPacked, MapZoneStyle style) {
        final long key = ((long) (terrainPacked & 0xFF) << 32) | (style.cacheKey() & 0xFFFFFFFFL);
        return cache.computeIfAbsent(key, ignored -> compute(terrainPacked, style));
    }

    private byte compute(byte terrainPacked, MapZoneStyle style) {
        final Color terrain = MapPalette.getColor(terrainPacked);
        final Color tint = style.getColor();
        final double alpha = style.getAlpha();

        final int red = clamp(tint.getRed() * alpha + terrain.getRed() * (1 - alpha));
        final int green = clamp(tint.getGreen() * alpha + terrain.getGreen() * (1 - alpha));
        final int blue = clamp(tint.getBlue() * alpha + terrain.getBlue() * (1 - alpha));
        return MapPalette.matchColor(new Color(red, green, blue));
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    /** Drops the blend cache (e.g. when styles are reloaded). */
    public void invalidate() {
        cache.clear();
    }
}
