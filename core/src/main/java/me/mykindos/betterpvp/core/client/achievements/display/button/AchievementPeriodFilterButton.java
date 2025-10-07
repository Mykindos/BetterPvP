package me.mykindos.betterpvp.core.client.achievements.display.button;

import lombok.CustomLog;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.display.AchievementMenu;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriod;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CustomLog
public class AchievementPeriodFilterButton extends AbstractItem {
    private final IAchievementCategory achievementCategory;
    private final Client client;
    private final AchievementManager achievementManager;
    private final StatPeriodManager statPeriodManager;
    @Setter
    private Windowed current;

    private final List<StatPeriod> periods;
    private final int selected;
    private final int numToShow;
    public AchievementPeriodFilterButton(IAchievementCategory achievementCategory, Client client, String period, AchievementManager achievementManager, StatPeriodManager statPeriodManager) {
        //todo convert to control item
        this.achievementCategory = achievementCategory;
        this.client = client;
        this.achievementManager = achievementManager;
        this.periods = statPeriodManager.getObjects().values().stream().sorted().toList();
        this.selected = Collections.binarySearch(periods, statPeriodManager.getObject(period).orElseThrow());
        this.statPeriodManager = statPeriodManager;
        this.numToShow = 9;
    }

    private StatPeriod increase() {
        int newSelected = selected + 1;
        if (newSelected >= periods.size()) {
            newSelected = 0;
        }
        return periods.get(newSelected);
    }

    private StatPeriod decrease() {
        int newSelected = selected - 1;
        if (newSelected < 0) {
            newSelected = periods.size() - 1;
        }
        return periods.get(newSelected);
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
        StatPeriod period = null;
        if (clickType.isLeftClick()) {
            period = increase();
        }
        if (clickType.isRightClick()) {
            period = decrease();
        }
        if (period != null) {
            final String strPeriod = period.equals(StatPeriodManager.GLOBAL_PERIOD) ? StatContainer.GLOBAL_PERIOD_KEY : period.getPeriod();
            new AchievementMenu(client, achievementCategory, strPeriod, achievementManager, statPeriodManager, current).show(player);
        }
    }

    @Override
    public ItemProvider getItemProvider() {
        List<Component> lore = new ArrayList<>();
        if (!periods.isEmpty()) {
            //put the selected value in the middle, cannot be less than 0
            int min = Math.max(0, selected - numToShow/2);
            //dont scroll down if there is no more values to show
            if (min + numToShow >= periods.size()) {
                min = Math.max(0, periods.size() - numToShow);
            }
            //get the max element in this view, that we are showing
            int max = Math.min(min + numToShow, periods.size());

            //add it to lore
            for (int i = min; i < max; i++) {
                if (i == selected) {
                    lore.add(Component.text(periods.get(i).getPeriod() + " \u00AB", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(periods.get(i).getPeriod(), NamedTextColor.GRAY));
            }
        }


        return ItemView.builder()
                .displayName(Component.text("Period", NamedTextColor.WHITE, TextDecoration.BOLD))
                .material(Material.ANVIL)
                .customModelData(0)
                .lore(lore)
                .action(ClickActions.LEFT, Component.text("Next Period"))
                .action(ClickActions.RIGHT, Component.text("Previous Period"))
                .build();
    }
}
