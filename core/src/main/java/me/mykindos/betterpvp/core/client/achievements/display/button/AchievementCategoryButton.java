package me.mykindos.betterpvp.core.client.achievements.display.button;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.category.IAchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.display.AchievementMenu;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AchievementCategoryButton extends AbstractItem {
    private final IAchievementCategory achievementCategory;
    private final AchievementManager achievementManager;
    private final RealmManager realmManager;
    private final Client client;
    @NotNull
    private final StatFilterType type;
    @Nullable
    private final Period period;
    private final Windowed current;

    public AchievementCategoryButton(IAchievementCategory achievementCategory, @NotNull StatFilterType type, @Nullable Period period, Client client, AchievementManager achievementManager, RealmManager realmManager, Windowed current) {
        this.achievementCategory = achievementCategory;
        this.realmManager = realmManager;
        this.achievementManager = achievementManager;
        this.client = client;
        this.type = type;
        this.period = period;
        this.current = current;
    }

    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider() {
            ItemView itemView = achievementCategory.getItemView();
            if (!achievementCategory.getChildren().isEmpty()) {
                return itemView.toBuilder()
                        .action(ClickActions.ALL, Component.text("Show Sub-Category"))
                        .build();
            }
            return itemView.toBuilder()
                    .action(ClickActions.ALL, Component.text("Show Achievements"))
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
        new AchievementMenu(client, achievementCategory, type, period, achievementManager, realmManager, current).show(player);
    }
}
