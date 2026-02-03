package me.mykindos.betterpvp.core.client.stats.display.general;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QualifiedStatListButton extends ControlItem<IAbstractStatMenu> {
    private final List<IStat> stats;
    private final int numPerItem;
    private final int pageNum;

    public QualifiedStatListButton(List<IStat> stats, int numPerItem, int pageNum) {
        this.stats = stats;
        this.numPerItem = numPerItem;
        this.pageNum = pageNum;

    }

    /**
     * Indicates whether this button is unused (i.e., has no stats to display).
     * @return true if this button is unused; false otherwise.
     */
    public boolean unused() {
        int startIndex = pageNum * numPerItem;
        return startIndex >= stats.size();
    }

    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {
        if (unused()) {
            return ItemView.EMPTY;
        }
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final StatFilterType statFilterType = gui.getType();
        final Period period = gui.getPeriod();
        int startIndex = pageNum * numPerItem;

        int endIndex = Math.min(startIndex + numPerItem, stats.size());

        List<Component> description = stats.stream()
                .skip(startIndex)
                .limit(endIndex - startIndex)
                .map(stat -> UtilMessage.deserialize("<gold>%s</gold>: <yellow>%s</yellow>",
                        stat.getQualifiedName(), stat.formattedStatValue(statContainer, statFilterType, period))
                )
                .toList();
        return ItemView.builder()
                .displayName(Component.text("Stat List " + (pageNum + 1)))
                .material(Material.PAPER)
                .lore(description)
                .frameLore(true)
                .build();
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        //no click action
    }
}
