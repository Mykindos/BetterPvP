package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Value;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * How a zone or clan territory is painted: one or more {@link MapFillStyle}s drawn together, so an operator can write
 * {@code style: BORDER, DIAGONAL} and get a hatched region inside a hard outline. The styles are a union — a pixel is
 * painted if any of them paints it — which is what makes them compose without an ordering rule.
 */
@Value
public class MapFill {

    Set<MapFillStyle> styles;

    private MapFill(Set<MapFillStyle> styles) {
        this.styles = styles.isEmpty() ? EnumSet.of(MapFillStyle.FILL) : EnumSet.copyOf(styles);
    }

    public static MapFill of(MapFillStyle... styles) {
        return new MapFill(styles.length == 0 ? EnumSet.noneOf(MapFillStyle.class) : EnumSet.of(styles[0], styles));
    }

    /**
     * @param raw      a config value: one style name, or several separated by commas, pluses or spaces
     * @param fallback the fill to use when {@code raw} is null, empty, or names nothing recognisable
     * @return the parsed fill
     */
    public static MapFill parse(String raw, MapFill fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        final EnumSet<MapFillStyle> parsed = EnumSet.noneOf(MapFillStyle.class);
        for (String token : raw.split("[,+\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            try {
                parsed.add(MapFillStyle.valueOf(token.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                // An unknown name is ignored rather than failing the whole fill, so one typo cannot hide a zone.
            }
        }
        return parsed.isEmpty() ? fallback : new MapFill(parsed);
    }

    public boolean has(MapFillStyle style) {
        return styles.contains(style);
    }

    /**
     * @return whether any style in this fill needs its neighbourhood, so a caller can skip the probe entirely
     */
    public boolean needsEdges() {
        for (MapFillStyle style : styles) {
            if (style.needsEdges()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param gridX the pixel's x on the sample grid
     * @param gridZ the pixel's z on the sample grid
     * @param edges the pixel's neighbourhood, or {@link MapZoneEdges#NONE} when {@link #needsEdges()} is false
     * @return whether any style paints the pixel
     */
    public boolean paintsPixel(int gridX, int gridZ, MapZoneEdges edges) {
        for (MapFillStyle style : styles) {
            if (style.paintsPixel(gridX, gridZ, edges)) {
                return true;
            }
        }
        return false;
    }
}
