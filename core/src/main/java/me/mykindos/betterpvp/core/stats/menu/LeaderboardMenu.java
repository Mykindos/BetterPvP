package me.mykindos.betterpvp.core.stats.menu;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class LeaderboardMenu extends Menu implements IRefreshingMenu {

    private static final int POSITION_INDEX = UtilMessage.DIVIDER.content().length() / 2 - 6;
    private final Leaderboard<?, ?> leaderboard;
    private int sortTypeIndex;

    public LeaderboardMenu(Player player, Leaderboard<?, ?> leaderboard) {
        super(player, 54, Component.text(leaderboard.getName()));
        this.leaderboard = leaderboard;
        this.sortTypeIndex = 0;
        refresh();
    }

    @Override
    public void refresh() {
        // Sort button
        List<Component> sortTypeLore = new ArrayList<>();
        final SortType[] sortTypes = leaderboard.acceptedSortTypes();
        SortType selectedSortType = sortTypes[sortTypeIndex];
        for (SortType type : sortTypes) {
            String name = type.getName();
            NamedTextColor color = NamedTextColor.GRAY;
            final boolean isSelected = selectedSortType == type;
            if (isSelected) {
                name += " \u00AB";
                color = NamedTextColor.GREEN;
            }
            sortTypeLore.add(Component.text(name, color));
        }

        final TextComponent sortTypeName = Component.text("Sort", NamedTextColor.WHITE, TextDecoration.BOLD);
        addButton(new Button(0, new ItemStack(Material.ARMOR_STAND), sortTypeName, sortTypeLore) {
            @Override
            public void onClick(Player player, Gamer gamer, ClickType clickType) {
                sortTypeIndex = sortTypeIndex + 1 >= leaderboard.acceptedSortTypes().length ? 0 : sortTypeIndex + 1;
                LeaderboardMenu.this.refresh();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 2f);
            }

            @Override
            public double getClickCooldown() {
                return 0.5;
            }
        });

        // Standings
        int position = 0;
        final ArrayList<? extends LeaderboardEntry<?, ?>> entries = new ArrayList<>(leaderboard.getTopTen(selectedSortType));
        for (int i = 0; i < 10; i++) {
            position++;
            final TextColor color = getPositionColor(position);
            final TextComponent title = Component.text(" ".repeat(POSITION_INDEX) + "#" + position, color, TextDecoration.BOLD);
            int slot = switch (position) {
                case 1 -> 4;
                case 2 -> 12;
                case 3 -> 14;
                default -> 36 + (position - 3);
            };

            if (entries.size() <= i) {
                addButton(getEmpty(slot, title));
                continue;
            }

            final LeaderboardEntry<?, ?> entry = entries.get(i);
            final Object key = entry.getKey();
            final Object value = entry.getValue();

            ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
            final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
            String keyRender = key.toString();
            if (key instanceof UUID uuid) {
                final OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                meta.setPlayerProfile(player.getPlayerProfile());
                keyRender = player.getName();
            } else if (key instanceof OfflinePlayer entryPlayer) {
                meta.setPlayerProfile(entryPlayer.getPlayerProfile());
                keyRender = entryPlayer.getName();
            }

            itemStack.setItemMeta(meta);

            if (value instanceof Number number) {
                addButton(getEntry(slot, title, itemStack, keyRender, NumberFormat.getInstance().format(number)));
            } else {
                addButton(getEntry(slot, title, itemStack, keyRender, value.toString()));
            }

        }

        // Podium spots
        addButton(new Button(13,
                new ItemStack(Material.GOLD_INGOT),
                Component.text("Top #1", getPositionColor(1), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)));
        addButton(new Button(21,
                new ItemStack(Material.IRON_INGOT),
                Component.text("Top #2", getPositionColor(2), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)));
        addButton(new Button(23,
                new ItemStack(Material.COPPER_INGOT),
                Component.text("Top #3", getPositionColor(3), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)));

        // Own data
        try {
            final Leaderboard<UUID, ?> lb = (Leaderboard<UUID, ?>) leaderboard;
            final CompletableFuture<?> entryData = lb.getEntryData(selectedSortType, player.getUniqueId());
            final Button button = new Button(49,
                    new ItemStack(Material.PAPER),
                    Component.text("Retrieving your data...", NamedTextColor.GRAY));

            entryData.whenComplete((data, throwable) -> {
                if (throwable != null) {
                    button.setItemStack(getFailedItem(button.getItemStack()));
                    return;
                }

                final Button replacement = getEntry(49,
                        Component.text("Your Data", NamedTextColor.WHITE, TextDecoration.BOLD),
                        button.getItemStack(),
                        player.getName(),
                        data.toString());
                button.setItemStack(replacement.getItemStack());
                LeaderboardMenu.this.refreshButton(button);
            }).exceptionally(throwable -> {
                log.error("Failed to retrieve leaderboard data for " + player.getName(), throwable);
                button.setItemStack(getFailedItem(button.getItemStack()));
                return null;
            });

            addButton(button);
        } catch (ClassCastException ignored) {
            // Not a leaderboard with UUID keys aka players
        }

        // Fill empty
        fillEmpty(Menu.BACKGROUND);
    }

    private ItemStack getFailedItem(ItemStack itemStack) {
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Failed to retrieve your data!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private Button getEmpty(int slot, Component title) {
        final ItemStack itemStack = new ItemStack(Material.SKELETON_SKULL);
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                UtilMessage.DIVIDER,
                Component.empty(),
                UtilMessage.deserialize("<red><bold>EMPTY POSITION!").decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                UtilMessage.DIVIDER));
        itemStack.setItemMeta(meta);
        return new Button(slot, itemStack);
    }

    private Button getEntry(int slot, Component title, ItemStack itemStack, String key, String data) {
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                UtilMessage.DIVIDER,
                Component.empty(),
                UtilMessage.deserialize("<white>Holder: <gray>%s", key).decoration(TextDecoration.ITALIC, false),
                UtilMessage.deserialize("<white>%s: <gray>%s", leaderboard.getName(), data).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                UtilMessage.DIVIDER));
        itemStack.setItemMeta(meta);
        return new Button(slot, itemStack);
    }

    private TextColor getPositionColor(int positionIndex) {
        return switch (positionIndex) {
            case 1 -> TextColor.color(255, 236, 89);
            case 2 -> TextColor.color(217, 217, 217);
            case 3 -> TextColor.color(255, 169, 64);
            default -> NamedTextColor.GREEN;
        };
    }
}
