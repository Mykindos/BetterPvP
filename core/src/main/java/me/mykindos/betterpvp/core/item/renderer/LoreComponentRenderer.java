package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.framework.adapter.Compatibility;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import me.mykindos.betterpvp.core.item.component.impl.purity.PurityComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.*;

/**
 * Renderer for displaying lore on items. This reads all {@link me.mykindos.betterpvp.core.item.component.LoreComponent}s
 * from the item and writes them to the item stack.
 */
public class LoreComponentRenderer implements ItemLoreRenderer {

    @Override
    public List<Component> create(ItemInstance item, ItemStack itemStack, int page) {
        final List<Component> components = item.getComponents().stream()
                .filter(component -> component instanceof LoreComponent)
                .map(component -> (LoreComponent) component)
                .filter(component -> component.getLorePage() == page)
                .sorted(Comparator.comparingInt(LoreComponent::getRenderPriority))
                .flatMap(component -> {
                    List<Component> lines = new ArrayList<>(component.getLines(item));

                    if (lines.isEmpty()) {
                        return lines.stream();
                    }
                    lines.addFirst(Component.empty());
                    return lines.stream(); // Add a separator after each component
                })
                .map(line -> line.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toCollection(ArrayList::new)); // Mutable list to allow removing the last element

        // Purity ONLY if NEXO is available AND item is attuned
        final Optional<PurityComponent> purityComponent = item.getComponent(PurityComponent.class);
        if (Compatibility.TEXTURE_PROVIDER && purityComponent.isPresent() && purityComponent.get().isAttuned()) {
            final ItemPurity purity = purityComponent.get().getPurity();
            components.addFirst(purity.createLoreComponent());
        }

        // Normalize font - replace all elements in the components list
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            Style style = component.style().font(Key.key("betterpvp", "rpg"));
            components.set(i, component.applyFallbackStyle(style));
        }

        components.add(Component.empty());

        // Pages
        final List<Integer> pages = LorePages.visiblePages(item);
        if (pages.size() > 1) {
            // make it lighter
            TextColor highlightColor = item.getRarity().getColor();
            highlightColor = TextColor.color(
                    Math.min(255, highlightColor.red() + 75),
                    Math.min(255, highlightColor.green() + 75),
                    Math.min(255, highlightColor.blue() + 75)
            );

            TextColor dimColor = TextColor.color(143, 143, 143);

            final TextComponent.Builder pagesComponent = Component.text();
            for (int i : pages) {
                TextColor color = i == page ? highlightColor : dimColor;
                pagesComponent.append(Component.empty()
                        .append(Component.translatable("space.2").font(SPACE))
                        .append(Component.text("●", color)));
            }

            components.add(Component.empty()
                    .append(Component.translatable("space.127").font(SPACE))
                    .append(pagesComponent.build())
                    .append(Component.translatable("space.5").font(SPACE))
                    .append(Component.text("\uE7EB", NamedTextColor.WHITE).font(INPUT))
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Rarity ONLY if NEXO is available
        if (Compatibility.TEXTURE_PROVIDER) {
            components.add(Component.text(item.getRarity().getGlyph(), NamedTextColor.WHITE).font(NEXO).decoration(TextDecoration.ITALIC, false));
        }

        return components;
    }

}
