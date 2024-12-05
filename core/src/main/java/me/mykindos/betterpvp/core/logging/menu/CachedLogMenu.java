package me.mykindos.betterpvp.core.logging.menu;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.button.CachedLogButton;
import me.mykindos.betterpvp.core.logging.menu.button.RefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterValueButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterButton;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterValueButton;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CachedLogMenu extends AbstractPagedGui<Item> implements Windowed {
    public static final List<String> CLANS = List.of(
            "All",
            LogContext.CLAN_NAME,
            LogContext.CLIENT_NAME
    );

    public static final List<String> CLANS_ADMIN = List.of(
            "All",
            LogContext.CLAN_NAME,
            LogContext.CLIENT_NAME,
            LogContext.CLAN,
            LogContext.CLIENT
    );

    public static final List<String> ITEM = List.of(
            "All",
            LogContext.CLIENT_NAME,
            LogContext.ITEM,
            LogContext.ITEM_NAME,
            LogContext.CLIENT
    );


    private final String title;
    private final String key;
    private final String value;
    private final @Nullable String actionFilter;
    private final BPvPPlugin plugin;
    private final LogRepository logRepository;
    private IStringFilterButton actionButton;
    private IStringFilterButton categoryButton;
    private IStringFilterValueButton valueButton;

    public CachedLogMenu(@NotNull String title, String key, String value, @Nullable String actionFilter, List<String> contexts, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        super(9, 5, false, new Structure(
                "# # # # # # A C V",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton())
                .addIngredient('R', new RefreshButton<>())
                .addIngredient('A', new StringFilterButton<>("Select Action", 9))
                .addIngredient('C', new StringFilterButton<>("Select Category", contexts, 9))
                .addIngredient('V', new StringFilterValueButton<>(9)
                )
        );

        if (getItem(8, 4) instanceof IRefreshButton refreshButton) {
            refreshButton.setRefresh(this::refresh);
        }

        if (getItem(6, 0) instanceof IStringFilterButton filterButton) {
            this.actionButton = filterButton;
            filterButton.setRefresh(this::refresh);
        }

        if (getItem(7, 0) instanceof IStringFilterButton filterButton) {
            this.categoryButton = filterButton;
            filterButton.setRefresh(this::refresh);
        }

        if (getItem(8, 0) instanceof IStringFilterValueButton logContextFilterValueButton) {
            this.valueButton = logContextFilterValueButton;
            logContextFilterValueButton.setRefresh(this::refresh);
        }


        this.title = title;
        this.key = key;
        this.value = value;
        this.actionFilter = actionFilter;
        this.plugin = plugin;
        this.logRepository = logRepository;
        setContent(List.of(new SimpleItem(ItemView.builder()
                .material(Material.PAPER)
                .displayName(Component.text("Loading..."))
                .build())
        ));
        refresh();
    }

    private CompletableFuture<Boolean> refresh() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        valueButton.setSelectedContext(categoryButton.getSelectedFilter());
        valueButton.getContextValues().clear();
        future.completeAsync(() -> {
            List<CachedLog> logs = logRepository.getLogsWithContextAndAction(key, value, actionFilter);
            if (LogContext.getAltContext(key) != null) {
                logs.addAll(logRepository.getLogsWithContextAndAction(LogContext.getAltContext(key), value, actionFilter));
                logs.sort(Comparator.comparingLong(CachedLog::getTimestamp).reversed());
            }

            logs.forEach(cachedLog -> {
                cachedLog.getContext().forEach((k, v) -> {
                    String altK = LogContext.getAltContext(k);
                    if (altK != null) {
                        valueButton.addValue(altK, v);
                    }
                    valueButton.addValue(k, v);

                    actionButton.add(cachedLog.getAction());
                });
            });
            List<Item> items = logs.stream()
                    .filter(cachedLog -> {
                        if (actionButton.getSelectedFilter().equals("All")) {
                            return true;
                        }
                        return Objects.equals(cachedLog.getAction(), actionButton.getSelectedFilter());
                    })
                    .filter(cachedLog -> {
                        String context = categoryButton.getSelectedFilter();
                        String altContext = LogContext.getAltContext(context);
                        String selectedValue = valueButton.getSelected();

                        Map<String, String> contextMap = cachedLog.getContext();

                        if (Objects.equals(context, "All")) {
                            return true;
                        }
                        return ((contextMap.containsKey(context) && contextMap.get(context).equals(selectedValue)) ||
                                contextMap.containsKey(altContext) && contextMap.get(altContext).equals(selectedValue));

                    })
                    .map(cachedLog -> new CachedLogButton(cachedLog, logRepository, this))
                    .map(Item.class::cast).toList();
            setContent(items);
            return true;
        });
        return future;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text(title);
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
