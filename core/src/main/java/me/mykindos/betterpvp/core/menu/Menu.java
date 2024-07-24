package me.mykindos.betterpvp.core.menu;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class Menu {

    /**
     * The {@link ItemProvider} for {@link ItemStack}s used as background items in menus.
     */
    public static final ItemProvider BACKGROUND_ITEM = ItemView.builder()
            .material(Material.GRAY_STAINED_GLASS_PANE)
            .displayName(Component.empty())
            .build();

    /**
     * The default {@link Item} implementation for background items in menus.
     */
    public static final Item BACKGROUND_GUI_ITEM = new SimpleItem(BACKGROUND_ITEM, click -> SoundEffect.WRONG_ACTION.play(click.getPlayer()));

    static {
        Structure.addGlobalIngredient('#', BACKGROUND_GUI_ITEM);
    }

    /**
     * Get a lore that fixes all lore lines to the same length, as to avoid
     * each line from going off the screen.
     *
     * @param loreIn The single-line lore description
     * @return An array of similar-length lore lines containing the lore description.
     */
    public static List<Component> getFixedLore(String loreIn) {
        String lore = loreIn;
        int lineLength = 30;
        lore = lore.trim();

        String[] lines = new String[(int) Math.ceil((double) lore.length() / lineLength)];
        int lineNumber = 0;

        while (!lore.trim().isEmpty()) {
            if (lines[lineNumber] == null) {
                lines[lineNumber] = "";
            }
            int wordEnd = lore.contains(" ") ? lore.indexOf(" ") + 1 : lore.length();
            String word = lore.substring(0, wordEnd);
            lore = lore.substring(wordEnd);
            lines[lineNumber] += word;
            if (lines[lineNumber].length() > lineLength) {
                lineNumber++;
            }
        }

        // Clearing lines that are empty and making them all gray
        return Arrays.stream(lines)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .map(line -> Component.text(line.trim(), NamedTextColor.GRAY).asComponent())
                .toList();
    }


}
