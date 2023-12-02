package me.mykindos.betterpvp.core.utilities.model.item.banner;

import org.bukkit.DyeColor;
import org.bukkit.Material;

import javax.annotation.Nonnull;

/**
 * Wrapper for Material enum to make it easier to get the banner type.
 */
public enum BannerColor {

    BLACK(Material.BLACK_BANNER),
    BLUE(Material.BLUE_BANNER),
    BROWN(Material.BROWN_BANNER),
    CYAN(Material.CYAN_BANNER),
    GRAY(Material.GRAY_BANNER),
    GREEN(Material.GREEN_BANNER),
    LIGHT_BLUE(Material.LIGHT_BLUE_BANNER),
    LIGHT_GRAY(Material.LIGHT_GRAY_BANNER),
    LIME(Material.LIME_BANNER),
    MAGENTA(Material.MAGENTA_BANNER),
    ORANGE(Material.ORANGE_BANNER),
    PINK(Material.PINK_BANNER),
    PURPLE(Material.PURPLE_BANNER),
    RED(Material.RED_BANNER),
    WHITE(Material.WHITE_BANNER),
    YELLOW(Material.YELLOW_BANNER);

    final Material material;

    BannerColor(final Material material) {
        this.material = material;
    }

    public static BannerColor fromType(@Nonnull final Material material) {
        for (BannerColor value : BannerColor.values()) {
            if (value.material == material) {
                return value;
            }
        }
        throw new IllegalArgumentException("Material " + material + " is not a banner!");
    }

    @Nonnull
    public static BannerColor fromDye(@Nonnull final DyeColor color) {
        return switch (color) {
            case BLACK -> BLACK;
            case BLUE -> BLUE;
            case BROWN -> BROWN;
            case CYAN -> CYAN;
            case GRAY -> GRAY;
            case GREEN -> GREEN;
            case LIGHT_BLUE -> LIGHT_BLUE;
            case LIGHT_GRAY -> LIGHT_GRAY;
            case LIME -> LIME;
            case MAGENTA -> MAGENTA;
            case ORANGE -> ORANGE;
            case PINK -> PINK;
            case PURPLE -> PURPLE;
            case RED -> RED;
            case WHITE -> WHITE;
            case YELLOW -> YELLOW;
        };
    }

}
