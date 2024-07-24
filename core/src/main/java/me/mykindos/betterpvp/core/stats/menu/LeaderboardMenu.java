package me.mykindos.betterpvp.core.stats.menu;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.filter.Filtered;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@CustomLog
@Getter
public class LeaderboardMenu<E, T> extends AbstractGui implements Windowed {

    private static final int POSITION_INDEX = UtilMessage.DIVIDER.content().length() / 2 - 6;

    private static TextColor getPositionColor(int positionIndex) {
        return switch (positionIndex) {
            case 1 -> TextColor.color(255, 236, 89);
            case 2 -> TextColor.color(217, 217, 217);
            case 3 -> TextColor.color(255, 169, 64);
            default -> NamedTextColor.GREEN;
        };
    }

    private final Windowed previous;
    private final Leaderboard<E, T> leaderboard;
    private SearchOptions searchOptions;
    @Getter(AccessLevel.NONE)
    private List<LeaderboardEntry<E, T>> entries = new ArrayList<>();
    private List<LeaderboardEntryButton<E, T>> buttons = new ArrayList<>();
    private final Player player;

    public LeaderboardMenu(Player player, Leaderboard<E, T> leaderboard, Windowed previous) {
        super(9, 6);
        this.leaderboard = leaderboard;
        this.player = player;
        this.previous = previous;
        populate();
        fetch();
    }

    private void populate() {
        // Buttons
        int buttonIndex = 0;
        SearchOptions.SearchOptionsBuilder optionsBuilder = SearchOptions.builder();
        if (leaderboard instanceof Sorted sorted) {
            // Create sort button
            final TextComponent sortTypeName = Component.text("Sort By", NamedTextColor.WHITE, TextDecoration.BOLD);
            CycleButton<SortType> button = new CycleButton<>(sorted.acceptedSortTypes(), new ItemStack(Material.ARMOR_STAND), sortTypeName, type -> {
                // On click, update search options
                searchOptions = searchOptions.withSort(type);
                fetch();
            });

            // Set values
            optionsBuilder.sort(button.getCurrent());
            setItem(buttonIndex, button);
            buttonIndex += 9;
        }

        if (leaderboard instanceof Filtered filtered) {
            // Create filter button
            final TextComponent filterTypeName = Component.text("Filter By", NamedTextColor.WHITE, TextDecoration.BOLD);
            CycleButton<FilterType> button = new CycleButton<>(filtered.acceptedFilters(), new ItemStack(Material.LECTERN), filterTypeName, type -> {
                // On click, update search options
                searchOptions = searchOptions.withFilter(type);
                fetch();
            });

            // Set values
            optionsBuilder.filter(button.getCurrent());
            setItem(buttonIndex, button);
        }

        // Back button
        if (previous != null) {
            setItem(45, new BackButton(previous));
        }

        // Standings
        this.searchOptions = optionsBuilder.build();
        for (int standing = 1; standing <= 10; standing++) {
            int slot = switch (standing) {
                case 1 -> 4;
                case 2 -> 12;
                case 3 -> 14;
                default -> 36 + (standing - 3);
            };

            final TextColor color = getPositionColor(standing);
            Component title = Component.text(" ".repeat(POSITION_INDEX) + "#" + standing, color, TextDecoration.BOLD);

            final ItemView failed = ItemView.builder().material(Material.BARRIER)
                    .displayName(title)
                    .lore(UtilMessage.DIVIDER)
                    .lore(Component.empty())
                    .lore(Component.text("        EMPTY POSITION!", NamedTextColor.RED, TextDecoration.BOLD))
                    .lore(Component.empty())
                    .lore(UtilMessage.DIVIDER)
                    .build();

            final ItemView loading = ItemView.builder().material(Material.PAPER)
                    .displayName(title)
                    .lore(UtilMessage.DIVIDER)
                    .lore(Component.empty())
                    .lore(Component.text("           LOADING...", NamedTextColor.RED, TextDecoration.BOLD))
                    .lore(Component.empty())
                    .lore(UtilMessage.DIVIDER)
                    .build();

            final int index = standing - 1;
            final LeaderboardEntryButton<E, T> button = new LeaderboardEntryButton<>(() -> {
                if (index >= entries.size()) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.completedFuture(entries.get(index));
            }, loading, failed, title);
            this.buttons.add(button);
            setItem(slot, button);
        }

        // Podium indicators
        final ItemView.ItemViewBuilder builder = ItemView.builder();
        final ItemView top1 = builder.material(Material.GOLD_INGOT).displayName(Component.text("Top #1", NamedTextColor.GOLD, TextDecoration.BOLD)).build();
        setItem(13, new SimpleItem(top1));
        final ItemView top2 = builder.material(Material.IRON_INGOT).displayName(Component.text("Top #2", NamedTextColor.GRAY, TextDecoration.BOLD)).build();
        setItem(21, new SimpleItem(top2));
        final ItemView top3 = builder.material(Material.COPPER_INGOT).displayName(Component.text("Top #3", NamedTextColor.GOLD, TextDecoration.BOLD)).build();
        setItem(23, new SimpleItem(top3));

        // Own data
        final TextComponent title = Component.text("Your Data", NamedTextColor.WHITE, TextDecoration.BOLD);
        final ItemView loading = ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Retrieving your data...", NamedTextColor.GRAY)).build();
        final LeaderboardEntryButton<E, T> button = new LeaderboardEntryButton<>(() -> {
            final CompletableFuture<Optional<LeaderboardEntry<E, T>>> data = leaderboard.getPlayerData(player.getUniqueId(), searchOptions);
            return data.thenApply(opt -> opt.orElse(null));
        }, loading, Menu.BACKGROUND_ITEM, title);
        this.buttons.add(button);
        setItem(49, button);

        setBackground(Menu.BACKGROUND_ITEM);
    }

    private void fetch() {
        this.entries = new ArrayList<>(leaderboard.getTopTen(searchOptions));
        this.buttons.forEach(LeaderboardEntryButton::fetch);
        updateControlItems();
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text(leaderboard.getName() + " Leaderboard");
    }

}
