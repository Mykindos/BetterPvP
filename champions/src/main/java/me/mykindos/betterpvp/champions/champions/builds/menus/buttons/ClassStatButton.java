package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassStat;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * One slot of the four-slot band that reads out a single {@link ClassStat} for the selected class as a
 * five-star rating. Every slot in the band carries the same tooltip; only the slot that {@code drawsBar} is
 * modelled as the bar itself, so hovering anywhere across the band tells the player the same thing.
 */
public class ClassStatButton extends ControlItem<ClassSelectionMenu> {

    private static final char STAR_SYMBOL = '★';
    private static final int DESCRIPTION_WIDTH = 35;

    private final ClassStat stat;
    private final boolean drawsBar;

    public ClassStatButton(ClassStat stat, boolean drawsBar) {
        this.stat = stat;
        this.drawsBar = drawsBar;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Purely a readout
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        final Role role = gui.getSelected();
        final int steps = stat.getSteps(role);

        final Component stars = ProgressBar.withLength((float) steps / ClassStat.STEPS, ClassStat.STEPS)
                .withCharacter(STAR_SYMBOL)
                .withProgressColor(NamedTextColor.YELLOW)
                .withRemainingColor(NamedTextColor.DARK_GRAY)
                .build();

        final Component displayName = Component.empty()
                .append(stat.getDisplayName().color(stat.getColor()).decorate(TextDecoration.BOLD))
                .appendSpace()
                .append(stars);
        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.PAPER)
                .displayName(displayName)
                .hideAdditionalTooltip(true);

        if (drawsBar) {
            builder.itemModel(Key.key("betterpvp", "menu/gui/classes/stat_bar"))
                    .customModelData((int) ((steps / (double) ClassStat.STEPS) * 20))
                    .dyedColor(stat.getDyeColor());
        } else {
            builder.itemModel(Resources.ItemModel.INVISIBLE)
                    .lore(Component.empty()); // bug with dyed color prepending a line to the lore
        }

        builder.lore(ComponentWrapper.markForWrap(stat.getDescription(), DESCRIPTION_WIDTH));
        builder.lore(Component.empty());
        return builder.build();
    }
}
