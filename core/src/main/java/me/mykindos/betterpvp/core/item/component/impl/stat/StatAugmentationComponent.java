package me.mykindos.betterpvp.core.item.component.impl.stat;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Component that holds a list of stat augmentations for an item.
 * <p>
 * Augmentations modify the base stats of an item through additions
 * or percentage increases. They are displayed in the item lore.
 */
public class StatAugmentationComponent extends AbstractItemComponent implements LoreComponent {

    @Getter
    private final List<StatAugmentation> augmentations;

    public StatAugmentationComponent() {
        super("stat-augmentation");
        this.augmentations = new ArrayList<>();
    }

    public StatAugmentationComponent(List<StatAugmentation> augmentations) {
        super("stat-augmentation");
        this.augmentations = new ArrayList<>(augmentations);
    }

    public StatAugmentationComponent withAugmentation(StatAugmentation augmentation) {
        augmentations.add(augmentation);
        return this;
    }

    @Override
    public ItemComponent copy() {
        return new StatAugmentationComponent(new ArrayList<>(augmentations));
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        final TextComponent description = Component.text("Apply this to an item at a Reforge NPC to augment its stats.", NamedTextColor.GRAY, TextDecoration.ITALIC);
        List<Component> lines = new ArrayList<>(ComponentWrapper.wrapLine(description));

        for (StatAugmentation augmentation : augmentations) {
            lines.add(Component.empty());
            final StatType<?> type = augmentation.getType();

            // Declare the type
            lines.add(Component.empty()
                    .append(Component.text("Stat:", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(type.getName(), type.getDisplayColor(), TextDecoration.UNDERLINED)));

            // Declare the value
            final TextColor tierColor = switch (augmentation.getTier()) {
                case 1 -> TextColor.color(171, 255, 193);
                case 2 -> TextColor.color(255, 248, 148);
                case 3 -> TextColor.color(255, 200, 71);
                default -> TextColor.color(255, 0, 0);
            };
            lines.add(Component.empty()
                    .append(Component.text("Augmentation:", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text("Tier " + UtilFormat.getRomanNumeral(augmentation.getTier()), tierColor)));
        }

        return lines;
    }

    @Override
    public int getRenderPriority() {
        return Integer.MAX_VALUE - 1; // Just before stats
    }
}
