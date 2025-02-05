package me.mykindos.betterpvp.clans.logging.menu;

import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
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
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@CustomLog
public class ClanKillLogMenu extends AbstractPagedGui<Item> implements Windowed {
    private final ReentrantLock updateLock = new ReentrantLock();
    private final Clan clan;
    private final ClanManager clanManager;
    private IStringFilterButton categoryButton;
    private IStringFilterValueButton valueButton;

    private List<KillClanLog> killLogs = new ArrayList<>();

    public ClanKillLogMenu(Clan clan, ClanManager clanManager) {
        super(9, 5, false, new Structure(
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
                .addIngredient('V', new StringFilterValueButton<>(9))
        );
        this.clan = clan;
        this.clanManager = clanManager;

        if (getItem(8, 4) instanceof IRefreshButton refreshButton) {
            refreshButton.setRefresh(this::refresh);
        }

        if (getItem(7, 0) instanceof IStringFilterButton filterButton) {
            this.categoryButton = filterButton;
            filterButton.setRefresh(this::sort);
        }

        if (getItem(8, 0) instanceof IStringFilterValueButton logContextFilterValueButton) {
            this.valueButton = logContextFilterValueButton;
            logContextFilterValueButton.setRefresh(this::sort);
        }
        setContent(List.of(new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Loading..."))
                .build())
        ));
        refresh();
    }

    /**
     * Updates and
     * @return completable future that completes as true when sort has been completed and applied
     */
    private CompletableFuture<Boolean> sort() {
        try {
            if (!updateLock.tryLock(30, TimeUnit.SECONDS)) {
                return CompletableFuture.completedFuture(false);
            }
            //TODO this might not need to be async
            return CompletableFuture.supplyAsync(() -> {
                valueButton.setSelectedContext(categoryButton.getSelectedFilter());
                valueButton.getContextValues().clear();
                killLogs.forEach(killClanLog -> {
                    valueButton.addValue("Clan", killClanLog.getKillerClanName());
                    valueButton.addValue("Clan", killClanLog.getVictimClanName());
                    valueButton.addValue("Client", killClanLog.getKillerName());
                    valueButton.addValue("Client", killClanLog.getVictimName());

                });
                List<Item> newContent = killLogs.stream()
                        .filter(killClanLog -> {
                            String context = categoryButton.getSelectedFilter();
                            String selectedValue = valueButton.getSelected();
                            if (Objects.equals(context, "All")) {
                                return true;
                            }
                            if (context.equals("Clan") && (killClanLog.getKillerClanName().equals(selectedValue) ||
                                    killClanLog.getVictimClanName().equals(selectedValue))) {
                                return true;
                            }

                            return context.equals("Client") && (killClanLog.getKillerName().equals(selectedValue) ||
                                    killClanLog.getVictimName().equals(selectedValue));
                        })
                        .map(killClanLog -> new ClanKillLogButton(clan, killClanLog, clanManager))
                        .map(Item.class::cast).toList();
                setContent(newContent);
                return true;
            });

        } catch (Exception ex) {
            log.error("Error loading clan kill logs", ex).submit();
            setContent(List.of(new SimpleItem(ItemView.builder()
                    .material(Material.BARRIER)
                    .displayName(Component.text("Error! Check console!"))
                    .lore(Component.text("Please inform staff if you see this"))
                    .build())));
            return CompletableFuture.completedFuture(false);
        }
        finally {
            updateLock.unlock();
        }
    }

    private CompletableFuture<Boolean> refresh() {
        if (!updateLock.tryLock()) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            CompletableFuture<List<KillClanLog>> future = clanManager.getRepository().getClanKillLogs(clan, clanManager);
            future = future.exceptionally((throwable -> {
                log.error("Error loading clan kill logs", throwable).submit();
                setContent(List.of(new SimpleItem(ItemView.builder()
                        .material(Material.BARRIER)
                        .displayName(Component.text("Error! Check console!"))
                        .lore(Component.text("Please inform staff if you see this"))
                        .build())));
                return List.of();
            }));
            //temporary menu
            PagedClanKillLogMenu pagedClanKillLogMenu = new PagedClanKillLogMenu(
                    clan,
                    categoryButton.getSelectedFilter(),
                    valueButton.getSelected(),
                    clanManager
            );
            pagedClanKillLogMenu.setPage(getCurrentPage());
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                findAllCurrentViewers().forEach(pagedClanKillLogMenu::show);
            });

            return future.thenApply(logs -> {
                this.killLogs = logs;
                sort().join();;
                setPage(pagedClanKillLogMenu.getCurrentPage());
                UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                    pagedClanKillLogMenu.findAllCurrentViewers().forEach(this::show);
                });

                return true;
            });

        }
        finally {
            updateLock.unlock();
        }
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
