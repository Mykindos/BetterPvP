package me.mykindos.betterpvp.core.item.renderer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.SPRITE;

/**
 * Changes the tooltip style attribute for items depending on their rarity
 */
public class RarityTooltipStyleRenderer implements ItemStackRenderer {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void write(ItemInstance item, ItemStack itemStack) {
        final long max = 155L;
        final ItemRarity rarity = item.getRarity();

        // calculate padding before and padding after
        final Component oldName = Objects.requireNonNull(itemStack.getData(DataComponentTypes.ITEM_NAME));
        final String text = PlainTextComponentSerializer.plainText().serialize(oldName);
        int width = 0;
        for (char c : text.toCharArray()) {
            switch (c) {
                case 'i' -> width += 2;
                case 'l' -> width += 3;
                case 'I', 't' -> width += 4;
                case 'f', 'k' -> width += 5;
                default  -> width += 6;
            }
        }
        if (!text.isEmpty()) width -= 1; // no trailing 1px gap after the last glyph
        final long length = width;
        // the max length is 150 of the entire line
        final long padding = (max - length) / 2;

        final Component centeredName = Component.empty()
                .append(Component.translatable("space." + padding).font(Resources.Font.SPACE))
                .append(oldName)
                .append(Component.translatable("space." + padding).font(Resources.Font.SPACE))
                .decoration(TextDecoration.ITALIC, false);

        // Items may carry an optional sprite to render above the name. When absent, the name
        // is left exactly as before.
        final List<Component> lines = new ArrayList<>();
        final Optional<TooltipSpriteComponent> spriteComponent = item.getComponent(TooltipSpriteComponent.class);
        if (spriteComponent.isPresent()) {
            // Sprites are 32x32, let's center it
            final int x = (int) ((max - 32) / 2);
            final Component sprite = Component.empty()
                    .append(Component.translatable("space." + x).font(Resources.Font.SPACE))
                    .append(spriteComponent.get().getSprite().applyFallbackStyle(NamedTextColor.WHITE));

            lines.add(Component.empty());
            lines.add(Component.empty());
            lines.add(Component.empty());
            lines.add(centeredName);
            itemStack.setData(DataComponentTypes.ITEM_NAME, sprite);
        } else {
            itemStack.setData(DataComponentTypes.ITEM_NAME, centeredName);
        }

        // We have to shift down by one
        lines.add(Component.text("\uEFFF", rarity.getColor()).font(SPRITE).shadowColor(ShadowColor.none()));

        ItemLore lore = itemStack.getData(DataComponentTypes.LORE);
        if (lore != null) lines.addAll(lore.lines());
        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));

        Key tooltipStyle = getTooltipStyle(rarity);
        if (spriteComponent.isPresent()) tooltipStyle = Key.key(tooltipStyle.namespace(), tooltipStyle.value());
        itemStack.setData(DataComponentTypes.TOOLTIP_STYLE, tooltipStyle);
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
