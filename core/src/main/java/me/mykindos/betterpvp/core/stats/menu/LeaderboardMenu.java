package me.mykindos.betterpvp.core.stats.menu;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.interfaces.IRefreshingMenu;
import me.mykindos.betterpvp.core.stats.Description;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.filter.Filtered;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class LeaderboardMenu<E, T> extends Menu implements IRefreshingMenu {

    private static final int POSITION_INDEX = UtilMessage.DIVIDER.content().length() / 2 - 6;
    private final Leaderboard<E, T> leaderboard;
    private final @Nullable CycleButton<SortType> sortButton;
    private final @Nullable CycleButton<FilterType> filterButton;

    public LeaderboardMenu(Player player, Leaderboard<E, T> leaderboard) {
        super(player, 54, Component.text(leaderboard.getName()));
        this.leaderboard = leaderboard;

        int buttonIndex = 0;
        if (leaderboard instanceof Sorted sorted) {
            final TextComponent sortTypeName = Component.text("Sort By", NamedTextColor.WHITE, TextDecoration.BOLD);
            sortButton = new CycleButton<>(0, sorted.acceptedSortTypes(), new ItemStack(Material.ARMOR_STAND), sortTypeName, this);
            buttonIndex += 9;
        } else {
            sortButton = null;
        }

        if (leaderboard instanceof Filtered filtered) {
            final TextComponent filterTypeName = Component.text("Filter By", NamedTextColor.WHITE, TextDecoration.BOLD);
            filterButton = new CycleButton<>(buttonIndex, filtered.acceptedFilters(), new ItemStack(Material.LECTERN), filterTypeName, this);
        } else {
            filterButton = null;
        }

        refresh();
    }

    @Override
    public void refresh() {
        SearchOptions.SearchOptionsBuilder optionsBuilder = SearchOptions.builder();
        if (sortButton != null) {
            addButton(sortButton);
            optionsBuilder.sort(sortButton.getCurrent());
        }

        if (filterButton != null) {
            addButton(filterButton);
            optionsBuilder.filter(filterButton.getCurrent());
        }

        // Standings
        final SearchOptions options = optionsBuilder.build();
        int position = 0;
        final ArrayList<? extends LeaderboardEntry<E, T>> entries = new ArrayList<>(leaderboard.getTopTen(options));
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


            final LeaderboardEntry<E, T> entry = entries.get(i);
            final Description description = leaderboard.getDescription(options, entry);
            addButton(getEntry(slot, title, description));
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
        final CompletableFuture<Optional<LeaderboardEntry<E, T>>> entryData = leaderboard.getPlayerData(player.getUniqueId(), options);
        final Button button = new Button(49,
                new ItemStack(Material.PAPER),
                Component.text("Retrieving your data...", NamedTextColor.GRAY));

        entryData.thenAccept(dataOpt -> {
            if (dataOpt.isEmpty()) {
                button.setItemStack(Menu.BACKGROUND);
                return;
            }

            final LeaderboardEntry<E, T> data = dataOpt.get();
            final Description description = leaderboard.getDescription(options, data);
            final Button replacement = getEntry(49,
                    Component.text("Your Data", NamedTextColor.WHITE, TextDecoration.BOLD),
                    description);
            button.setItemStack(replacement.getItemStack());
            LeaderboardMenu.this.refreshButton(button);
        }).exceptionally(throwable -> {
            log.error("Failed to retrieve leaderboard data for " + player.getName(), throwable);
            button.setItemStack(getFailedItem(button.getItemStack()));
            return null;
        });

        addButton(button);

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

    private Button getEntry(int slot, Component title, Description description) {
        final ItemStack itemStack = description.getIcon();
        final ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        final List<Component> lore = new ArrayList<>(List.of(
                UtilMessage.DIVIDER,
                Component.empty(),
                Component.empty(),
                UtilMessage.DIVIDER));

        // After "holder"
        int index = 2;
        for (Map.Entry<String, Component> entry : description.getLines().entrySet()) {
            final TextComponent keyText = Component.text(entry.getKey() + ": ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
            final Component valueText = entry.getValue().decoration(TextDecoration.ITALIC, false).applyFallbackStyle(Style.style(NamedTextColor.GRAY));
            lore.add(index, keyText.append(valueText));
            index++;
        }
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return new Button(slot, itemStack) {
            @Override
            public void onClick(Player player, Gamer gamer, ClickType clickType) {
                final Consumer<Player> clickFunction = description.getClickFunction();
                if (clickFunction == null) {
                    return;
                }
                clickFunction.accept(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            }
        };
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
