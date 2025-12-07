package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;

/**
 * This class is responsible for rendering the names of items based on their rarity.
 */
public class NameRarityRenderer implements ItemNameRenderer {

    private final Component name;

    public NameRarityRenderer(String name) {
        this(Component.text(name));
    }

    public NameRarityRenderer(Component name) {
        this.name = name;
    }

    @Override
    public Component createName(ItemInstance item) {
        return name.applyFallbackStyle(item.getRarity().getColor());
    }

}
