package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renderer for displaying lore on items. This reads all {@link me.mykindos.betterpvp.core.item.component.LoreComponent}s
 * from the item and writes them to the item stack.
 */
public class LoreComponentRenderer implements ItemLoreRenderer {

    @Override
    public List<Component> create(ItemInstance item, ItemStack itemStack) {
        final List<Component> components = item.getComponents().stream()
                .filter(component -> component instanceof LoreComponent)
                .map(component -> (LoreComponent) component)
                .sorted(Comparator.comparingInt(LoreComponent::getRenderPriority))
                .flatMap(component -> {
                    List<Component> lines = component.getLines(item);

                    if (lines.isEmpty()) {
                        return lines.stream();
                    }
                    lines.addFirst(Component.empty());
                    return lines.stream(); // Add a separator after each component
                })
                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toCollection(ArrayList::new)); // Mutable list to allow removing the last element

        // todo: add rarity icon

        return components;
    }

}
