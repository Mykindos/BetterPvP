package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Value;

import java.awt.Color;

/**
 * How one zone tag should look on the map: its blend colour, how strongly it tints, whether it shows at all, its
 * resolution priority when a location carries several styled tags, and the fill pattern to use. Built from the
 * {@code clans.map.zones.<tag>} config section by {@link me.mykindos.betterpvp.clans.clans.map.MapZoneStyleRegistry}.
 */
@Value
public class MapZoneStyle {

    String tag;
    Color color;
    double alpha;
    boolean visible;
    int priority;
    MapFill fill;

    /**
     * Whether this style paints in the per-pixel terrain tint layer. Chunk-shaped, separately-rendered tags (clan
     * territory) set this false so they are not also flat-filled under their own renderer.
     */
    boolean tintLayer;

    /**
     * @return a stable key over the visual inputs (colour + alpha), used to cache blended results
     */
    public int cacheKey() {
        return color.getRGB() * 31 + Double.hashCode(alpha);
    }
}
