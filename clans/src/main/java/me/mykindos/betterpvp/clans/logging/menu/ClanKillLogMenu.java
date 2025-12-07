package me.mykindos.betterpvp.clans.logging.menu;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.logging.KillClanLog;
import me.mykindos.betterpvp.clans.logging.button.ClanKillLogButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.logging.menu.button.RefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterValueButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterValueButton;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.PageForwardButton;
import me.mykindos.betterpvp.core.menu.button.PageBackwardButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class ClanKillLogMenu extends AbstractPagedGui<Item> implements Windowed {

    private static final String FILTER_ALL = "All";
    private static final String FILTER_CLAN = "Clan";
    private static final String FILTER_CLIENT = "Client";

    private final Clan clan;
    private final ClanManager clanManager;

    private IStringFilterButton categoryButton;
    private IStringFilterValueButton valueButton;

    public ClanKillLogMenu(Clan clan, ClanManager clanManager) {
        super(9, 5, false, new Structure(
                "# # # # # # # C V",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PageBackwardButton())
                .addIngredient('-', new BackButton(null))
                .addIngredient('>', new PageForwardButton())
                .addIngredient('R', new RefreshButton<>())
                .addIngredient('C', new StringFilterButton<>("Select Category", List.of(FILTER_ALL, FILTER_CLAN, FILTER_CLIENT), 9, Material.WRITABLE_BOOK, 0))
                .addIngredient('V', new StringFilterValueButton<>(9))
        );
        this.clan = clan;
        this.clanManager = clanManager;

        if (getItem(8, 4) instanceof IRefreshButton refreshButton) {
            refreshButton.setRefresh(this::refresh);
        }

        if (getItem(7, 0) instanceof IStringFilterButton filterButton) {
            this.categoryButton = filterButton;
            filterButton.setRefresh(this::refresh);
        }

        if (getItem(8, 0) instanceof IStringFilterValueButton logContextFilterValueButton) {
            this.valueButton = logContextFilterValueButton;
            logContextFilterValueButton.setRefresh(this::refresh);
        }
        setContent(List.of(new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Loading..."))
                .build())
        ));
        refresh();
    }

    private CompletableFuture<Boolean> refresh() {
        valueButton.setSelectedContext(categoryButton.getSelectedFilter());
        valueButton.getContextValues().clear();

        return CompletableFuture.supplyAsync(() -> {
            List<KillClanLog> logs = clanManager.getRepository().getClanKillLogs(clan, clanManager);

            // Populate filter values
            populateFilterValues(logs);

            return logs.stream()
                    .filter(this::matchesSelectedFilter)
                    .map(killClanLog -> new ClanKillLogButton(clan, killClanLog, clanManager))
                    .map(Item.class::cast)
                    .toList();
        }).exceptionally(throwable -> {
            log.error("Error loading clan kill logs for clan: {}", clan.getName(), throwable).submit();
            return List.of(new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Component.text("Error! Check console!"))
                    .lore(Component.text("Please inform staff if you see this"))
                    .build()));
        }).thenApply(logs -> {
            setContent(logs);
            return true;
        });
    }

    private void populateFilterValues(List<KillClanLog> logs) {
        logs.forEach(killClanLog -> {
            valueButton.addValue(FILTER_CLAN, killClanLog.getKillerClanName());
            valueButton.addValue(FILTER_CLAN, killClanLog.getVictimClanName());
            valueButton.addValue(FILTER_CLIENT, killClanLog.getKillerName());
            valueButton.addValue(FILTER_CLIENT, killClanLog.getVictimName());
        });
    }

    private boolean matchesSelectedFilter(KillClanLog killClanLog) {
        String context = categoryButton.getSelectedFilter();
        String selectedValue = valueButton.getSelected();

        if (Objects.equals(context, FILTER_ALL)) {
            return true;
        }

        if (FILTER_CLAN.equals(context)) {
            return killClanLog.getKillerClanName().equals(selectedValue) ||
                   killClanLog.getVictimClanName().equals(selectedValue);
        }

        if (FILTER_CLIENT.equals(context)) {
            return killClanLog.getKillerName().equals(selectedValue) ||
                   killClanLog.getVictimName().equals(selectedValue);
        }

        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(clan.getName() + "'s Kill Logs");
    }

    @Override
    public void bake() {
        int contentSize = getContentListSlots().length;

        List<List<SlotElement>> pages = new ArrayList<>();
        List<SlotElement> page = new ArrayList<>(contentSize);

        for (Item item : content) {
            page.add(new SlotElement.ItemSlotElement(item));

            if (page.size() >= contentSize) {
                pages.add(page);
                page = new ArrayList<>(contentSize);
            }
        }

        if (!page.isEmpty()) {
            pages.add(page);
        }

        this.pages = pages;
        update();
    }
}
