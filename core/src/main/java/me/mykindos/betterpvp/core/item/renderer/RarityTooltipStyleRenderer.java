package me.mykindos.betterpvp.core.item.renderer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Changes the tooltip style attribute for items depending on their rarity
 */
public class RarityTooltipStyleRenderer implements ItemStackRenderer {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void write(ItemInstance item, ItemStack itemStack) {
        final ItemRarity rarity = item.getRarity();
        final Key tooltipStyle = getTooltipStyle(rarity);
        itemStack.setData(DataComponentTypes.TOOLTIP_STYLE, tooltipStyle);

        // We have to shift down by one
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        ItemLore lore = itemStack.getData(DataComponentTypes.LORE);
        if (lore != null) lines.addAll(lore.lines());
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
    }

    private Key getTooltipStyle(ItemRarity itemRarity) {
        return switch (itemRarity) {
            case COMMON -> Key.key("betterpvp", "rarity/common");
            case UNCOMMON -> Key.key("betterpvp", "rarity/uncommon");
            case RARE -> Key.key("betterpvp", "rarity/rare");
            case EPIC -> Key.key("betterpvp", "rarity/epic");
            case LEGENDARY -> Key.key("betterpvp", "rarity/legendary");
            case MYTHICAL -> Key.key("betterpvp", "rarity/mythical");
        };
    }
}
