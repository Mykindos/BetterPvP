package me.mykindos.betterpvp.core.utilities;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

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
         * Input 16, used for item lore and menus
         */
        public static final Key INPUT = Key.key("betterpvp", "input/center");

        /**
         * 3d font, used for overlays
         */
        public static final Key FONT_3D = Key.key("betterpvp", "3d");

        /**
         * Menu
         * Used for the menu overlays.
         */
        public static final Key MENU = Key.key("betterpvp", "menu");

        /**
         * Nexo
         */
        public static final Key NEXO = Key.key("betterpvp", "nexo");

        /**
         * 32x32 item sprite glyphs rendered above item names.
         */
        public static final Key SPRITE = Key.key("betterpvp", "sprite");

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
     * Characters used as menu overlays in inventories. These only work with the {@link Resources.Font#INPUT} font,
     * and are ignored if used with any other font.
     */
    @UtilityClass
    public static class Font3d {

        public static Component of(@NotNull String text) {
            if (text.isEmpty()) {
                return Component.empty();
            }

            text = text.toLowerCase();
            if (!text.matches("[a-z]+")) {
                throw new IllegalArgumentException("Text must be all lowercase letters a-z");
            }

            // each letter turns into a single char ue1##, where ## is the letter's
            // index as two decimal digits placed in the low byte (a=01 -> \ue101, ..., z=26 -> \ue126)
            StringBuilder builder = new StringBuilder();
            for (char c : text.toCharArray()) {
                int index = c - 'a' + 1;  // a -> 1, ..., z -> 26
                int codepoint = 0xE100 + (index / 10) * 16 + (index % 10);
                builder.append((char) codepoint);
            }
            final String string = builder.toString();
            return Component.text(string, NamedTextColor.WHITE).font(Font.FONT_3D);
        }

    }

}
