package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;

/**
 * This class is responsible for rendering the names of items based on their rarity.
 */
public class NameComponentRenderer implements ItemNameRenderer {

    private final Component name;

    public NameComponentRenderer(Component name) {
        this.name = name;
    }

    @Override
    public Component createName(ItemInstance item) {
        return name;
    }
}
