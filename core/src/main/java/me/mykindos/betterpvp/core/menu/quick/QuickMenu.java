package me.mykindos.betterpvp.core.menu.quick;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.menu.viewer.GuiItemViewer;
import me.mykindos.betterpvp.core.recipe.crafting.menu.GuiCraftingTable;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import me.mykindos.betterpvp.core.stats.menu.LeaderboardCategoryMenu;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
public class QuickMenu {

    public static boolean isQuickMenuButton(ItemStack itemStack) {
        if (itemStack == null) return false;
        for (int i = 0; i < 4; i++) {
            ItemProvider button = getQuickMenuButton(i);
            if (button != null && button.get().equals(itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static ItemProvider getQuickMenuButton(int button) {
        return switch (button) {
            case 1 -> settingsButton();
            case 2 -> craftingButton();
            case 3 -> itemsButton();
            case 4 -> leaderboardButton();
            default -> null;
        };
    }

    public static boolean useQuickMenuButton(Client client, Player player, int button) {
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        return switch (button) {
            case 1 -> {
                UtilServer.runTask(plugin, () -> {
                    swapCursor(player, () -> new SettingsMenu(player, client).show(player));
                });
                yield true;
            }
            case 2 -> {
                UtilServer.runTask(plugin, () -> {
                    swapCursor(player, () -> {
                        final GuiCraftingTable gui = plugin.getInjector().getInstance(GuiCraftingTable.class);
                        gui.show(player);
                    });
                });
                yield true;
            }
            case 3 -> {
                UtilServer.runTask(plugin, () -> {
                    swapCursor(player, () -> {
                        final ItemFactory factory = plugin.getInjector().getInstance(ItemFactory.class);
                        new GuiItemViewer(factory).show(player);
                    });
                });
                yield true;
            }
            case 4 -> {
                UtilServer.runTask(plugin, () -> {
                    swapCursor(player, () -> {
                        final LeaderboardManager leaderboards = plugin.getInjector().getInstance(LeaderboardManager.class);
                        new LeaderboardCategoryMenu(leaderboards).show(player);
                    });
                });
                yield true;
            }
            default -> false;
        };
    }

    private static void swapCursor(Player player, Runnable runnable) {
        final ItemStack cursor = player.getItemOnCursor();
        player.setItemOnCursor(null);
        runnable.run();
        UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> player.setItemOnCursor(cursor));
    }

    private static ItemProvider craftingButton() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/crafting_table_icon"))
                .displayName(Component.text("Crafting Grid", TextColor.color(255, 149, 28), TextDecoration.BOLD))
                .build();
    }

    private static ItemProvider settingsButton() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/settings_icon"))
                .displayName(Component.text("Settings", TextColor.color(209, 209, 209), TextDecoration.BOLD))
                .lore(Component.text("You can also use", NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text("/settings", NamedTextColor.WHITE)))
                .build();
    }

    private static ItemProvider itemsButton() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/chest_icon"))
                .displayName(Component.text("Items & Recipes", TextColor.color(255, 205, 66), TextDecoration.BOLD))
                .lore(Component.text("You can also use", NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text("/items", NamedTextColor.YELLOW)))
                .build();
    }

    private static ItemProvider leaderboardButton() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/icon/regular/rank_icon"))
                .displayName(Component.text("Leaderboards", TextColor.color(158, 255, 66), TextDecoration.BOLD))
                .lore(Component.text("You can also use", NamedTextColor.GRAY)
                        .appendSpace()
                        .append(Component.text("/top", NamedTextColor.GREEN)))
                .build();
    }

}
