package me.mykindos.betterpvp.core.client.achievements.display;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AchievementScopeMenu extends AbstractGui implements Windowed {

    private final Client client;
    private final AchievementManager achievementManager;
    private final RealmManager realmManager;
    @Nullable
    private final Windowed previous;

    public AchievementScopeMenu(@NotNull Client client, @NotNull AchievementManager achievementManager, @NotNull RealmManager realmManager, @Nullable Windowed previous) {
        super(9, 3);
        this.client = client;
        this.achievementManager = achievementManager;
        this.realmManager = realmManager;
        this.previous = previous;

        applyStructure(new Structure(
                "# # # # # # # # #",
                "# # G # # # S # #",
                "# # # # B # # # #")
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('G', new GlobalAchievementsButton())
                .addIngredient('S', new SeasonalAchievementsButton())
                .addIngredient('B', new BackButton(previous)));
    }

    @Override
    public @NotNull Component getTitle() {
        return Translations.component("core.menu.achievement.scope.title");
    }

    private class GlobalAchievementsButton extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.NETHER_STAR)
                    .displayName(Translations.component("core.menu.achievement.scope.button.global.name").color(NamedTextColor.YELLOW))
                    .lore(Translations.component("core.menu.achievement.scope.button.global.lore.1").color(NamedTextColor.GRAY))
                    .action(ClickActions.ALL, Translations.component("core.menu.achievement.scope.button.global.action"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new AchievementMenu(client, null, StatFilterType.ALL, null, achievementManager, realmManager, AchievementScopeMenu.this).show(player);
        }
    }

    private class SeasonalAchievementsButton extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.CLOCK)
                    .displayName(Translations.component("core.menu.achievement.scope.button.seasonal.name").color(NamedTextColor.YELLOW))
                    .lore(Translations.component("core.menu.achievement.scope.button.seasonal.lore.1").color(NamedTextColor.GRAY))
                    .action(ClickActions.ALL, Translations.component("core.menu.achievement.scope.button.seasonal.action"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new AchievementMenu(client, null, StatFilterType.SEASON, Core.getCurrentRealm().getSeason(), achievementManager, realmManager, AchievementScopeMenu.this).show(player);
        }
    }
}
