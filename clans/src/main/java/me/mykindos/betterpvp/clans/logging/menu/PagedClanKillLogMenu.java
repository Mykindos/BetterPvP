package me.mykindos.betterpvp.clans.logging.menu;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.button.ClanKillLogButton;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.logging.menu.button.RefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterValueButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterValueButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.menu.impl.PagedCollectionMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PagedClanKillLogMenu extends PagedCollectionMenu<ClanKillLogButton> implements Windowed {
    private final ClanManager clanManager;
    private final Clan clan;
    private @Nullable String filterType;
    private @Nullable String filterValue;

    private IStringFilterButton categoryButton;
    private IStringFilterValueButton valueButton;

    protected PagedClanKillLogMenu(Clan clan, @Nullable String filterType, @Nullable String filterValue, ClanManager clanManager) {
        super(new Structure(
                "# # # # # # # C V",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(null))
                .addIngredient('>', new ForwardButton())
                .addIngredient('R', new RefreshButton<>())
                .addIngredient('C', new StringFilterButton<>("Select Category", List.of("All", "Clan", "Client"), 9, Material.WRITABLE_BOOK, 0))
                .addIngredient('V', new StringFilterValueButton<>(9)));
        this.clan = clan;
        this.filterType = filterType;
        if (this.filterType != null && filterType.equalsIgnoreCase("All")) {
            this.filterType = null;
        }
        this.filterValue = filterValue;
        this.clanManager = clanManager;
        if (getItem(8, 4) instanceof IRefreshButton refreshButton) {
            refreshButton.setRefreshing(true);
        }

        if (getItem(7, 0) instanceof IStringFilterButton filterButton) {
            this.categoryButton = filterButton;
            this.categoryButton.setStatic(true);
            if (filterType == null) filterType = "All";
            switch (filterType) {
                case ("Clan") ->
                    this.categoryButton.setSelected(1);
                case ("Client") ->
                    this.categoryButton.setSelected(2);
                default ->
                    this.categoryButton.setSelected(0);
            }
        }

        if (getItem(8, 0) instanceof IStringFilterValueButton logContextFilterValueButton) {
            this.valueButton = logContextFilterValueButton;
            this.valueButton.setStatic(true);
            if (filterValue != null) {
                this.valueButton.addValue(filterType, filterValue);
                this.valueButton.setSelectedContext(filterType);
            }
        }

        update();
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(clan.getName() + "'s Kill Logs");
    }

    /**
     * Retrieves the content for the specified page number
     *
     * @param page   the page number (0 is first)
     * @param amount (the number of items per page)
     * @return the list of items for the page
     */
    @Override
    protected CompletableFuture<List<ClanKillLogButton>> getPage(int page, int amount) {
            int start = page * amount;
            int end = (page + 1) * amount;
            return clanManager.getRepository().getClanKillLogs(
                            this.clan,
                            start,
                            end,
                            this.filterType,
                            this.filterValue,
                            this.clanManager
                    ).thenApply(logs -> logs.stream()
                    .map(log -> new ClanKillLogButton(clan, log, clanManager))
                    .toList());
    }
}
