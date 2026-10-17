package me.mykindos.betterpvp.clans.clans.map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.material.MapColor;

import java.awt.Color;

/**
 * Converts between packed map colours and RGB using the server's own map palette. Matching picks the nearest palette
 * entry by a red-mean weighted distance.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VanillaMapPalette {

    /** RGB per packed id, or -1 for ids the palette leaves empty. */
    private static final int[] RGB_BY_PACKED_ID = new int[256];

    static {
        for (int packed = 0; packed < RGB_BY_PACKED_ID.length; packed++) {
            final int argb = MapColor.getColorFromPackedId(packed);
            RGB_BY_PACKED_ID[packed] = (argb >>> 24) == 0 ? -1 : argb & 0xFFFFFF;
        }
    }

    /**
     * @param packed a packed map colour
     * @return its colour, fully transparent for an empty entry
     */
    public static Color getColor(byte packed) {
        final int rgb = RGB_BY_PACKED_ID[packed & 0xFF];
        return rgb == -1 ? new Color(0, true) : new Color(rgb);
    }

    /**
     * @param color any colour
     * @return the packed map colour closest to it, or {@code 0} (transparent) for a mostly transparent colour
     */
    public static byte matchColor(Color color) {
        if (color.getAlpha() < 128) {
            return 0;
        }

        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int packed = 0; packed < RGB_BY_PACKED_ID.length; packed++) {
            final int rgb = RGB_BY_PACKED_ID[packed];
            if (rgb == -1) {
                continue;
            }

            final long distance = distance(color, rgb);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = packed;
            }
        }
        return (byte) best;
    }

    private static long distance(Color color, int rgb) {
        final int red = (rgb >> 16) & 0xFF;
        final int green = (rgb >> 8) & 0xFF;
        final int blue = rgb & 0xFF;
        final int redSum = color.getRed() + red;
        final long dr = color.getRed() - red;
        final long dg = color.getGreen() - green;
        final long db = color.getBlue() - blue;
        return (1024L + redSum) * dr * dr + 2048L * dg * dg + (1024L + 510 - redSum) * db * db;
    }
}
