package me.mykindos.betterpvp.core.client.profile.menu;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.achievements.display.AchievementMenu;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.start.StartStatMenu;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

public class ProfileMenu extends AbstractGui implements Windowed {

    private final Client client;
    private final AchievementManager achievementManager;
    private final RealmManager realmManager;

    public ProfileMenu(Client client, AchievementManager achievementManager, RealmManager realmManager) {
        super(9, 4);
        this.client = client;
        this.achievementManager = achievementManager;
        this.realmManager = realmManager;

        Structure structure = new Structure(
                "# # # # # # # # #",
                "# . . S . A . . #",
                "# . . . . . . . #",
                "# # r r r r r # #")
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('S', new StatsButton())
                .addIngredient('A', new AchievementsButton())
                .addIngredient('r', Markers.CONTENT_LIST_SLOT_HORIZONTAL);

        applyStructure(structure);

        List<AchievementCompletion> recentAchievements = StreamSupport.stream(client.getStatContainer().getAchievementCompletions().spliterator(), false)
                .sorted(Comparator.comparing(AchievementCompletion::getTimestamp).reversed())
                .limit(5)
                .toList();

        refreshRecentAchievements(recentAchievements);
    }

    private void refreshRecentAchievements(List<AchievementCompletion> recentAchievements) {
        int x = 2;
        for (AchievementCompletion completion : recentAchievements) {
            IAchievement achievement = achievementManager.getObject(completion.getKey()).orElse(null);
            if (achievement != null) {
                setItem(x, 3, new RecentAchievementItem(achievement, completion));
            }
            x++;
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(client.getName() + "'s Profile");
    }

    private class StatsButton extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.BOOK)
                    .displayName(Component.text("Stats", NamedTextColor.YELLOW))
                    .lore(Component.text("View your detailed statistics", NamedTextColor.GRAY))
                    .action(ClickActions.ALL, Component.text("Open Stats Menu"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new StartStatMenu(client, ProfileMenu.this, StatFilterType.ALL, null, realmManager).show(player);
        }
    }

    private class AchievementsButton extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return ItemView.builder()
                    .material(Material.NETHER_STAR)
                    .displayName(Component.text("Achievements", NamedTextColor.YELLOW))
                    .lore(Component.text("View your earned achievements", NamedTextColor.GRAY))
                    .action(ClickActions.ALL, Component.text("Open Achievements Menu"))
                    .build();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new AchievementMenu(client, null, StatFilterType.ALL, null, achievementManager, realmManager, ProfileMenu.this).show(player);
        }
    }

    private static class RecentAchievementItem extends SimpleItem {
        public RecentAchievementItem(IAchievement achievement, AchievementCompletion completion) {
            super(ItemView.builder()
                    .material(achievement.getMaterial(completion.getClient().getStatContainer(), StatFilterType.ALL, null))
                    .displayName(Component.text("Recent Achievement: ", NamedTextColor.GREEN)
                            .append(Component.text(achievement.getName(), NamedTextColor.YELLOW)))
                    .lore(Component.text("Completed " + UtilTime.getTime(System.currentTimeMillis() - java.sql.Timestamp.valueOf(completion.getTimestamp()).getTime(), 1) + " ago", NamedTextColor.GRAY))
                    .build());
        }
    }
}
