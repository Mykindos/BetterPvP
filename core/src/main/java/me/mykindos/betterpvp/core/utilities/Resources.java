package me.mykindos.betterpvp.core.utilities;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;

/**
 * Utility class for all textures used in menus.
 */
@UtilityClass
public class Resources {

    /**
     * Available fonts with the server resource pack.
     */
    @UtilityClass
    public static class Font {

        /**
         * Used for all default text.
         */
        public static final Key DEFAULT = Key.key("minecraft", "default");

        /**
         * Used for invisible spacing in menus.
         */
        public static final Key SPACE = Key.key("space", "default");

        /**
         * Makes every character in the string small caps.
         */
        public static final Key SMALL_CAPS = Key.key("betterpvp", "small_caps");

        /**
         * Nexo
         */
        public static final Key NEXO = Key.key("betterpvp", "nexo");

    }

    /**
     * All item models that are used in the plugin.
     */
    @UtilityClass
    public static class ItemModel {
        public static final Key INVISIBLE = Key.key("betterpvp", "menu/invisible");
        public static final Key STOP = Key.key("betterpvp", "menu/icon/regular/stop");
    }

    /**
     * Characters used as menu overlays in inventories. These only work with the {@link Resources.Font#MENUS} font.
     */
    @UtilityClass
    public static class MenuFontCharacter {
        public static final char HOT_BAR_LAYOUT = '✢';
        public static final char SELECT_ONE = '≭';
    }

}
