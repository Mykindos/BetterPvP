package me.mykindos.betterpvp.core.logging.menu;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.inventory.gui.AbstractPagedGui;
import me.mykindos.betterpvp.core.inventory.gui.SlotElement;
import me.mykindos.betterpvp.core.inventory.gui.structure.Markers;
import me.mykindos.betterpvp.core.inventory.gui.structure.Structure;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.menu.button.CachedLogButton;
import me.mykindos.betterpvp.core.logging.menu.button.LogContextFilterCategoryButton;
import me.mykindos.betterpvp.core.logging.menu.button.LogContextFilterValueButton;
import me.mykindos.betterpvp.core.logging.menu.button.RefreshButton;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.menu.button.ForwardButton;
import me.mykindos.betterpvp.core.menu.button.PreviousButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CachedLogMenu extends AbstractPagedGui<Item> implements Windowed {
    private final String title;
    private final String key;
    private final String value;
    private final @Nullable String actionFilter;
    private final BPvPPlugin plugin;
    private final LogRepository logRepository;
    private LogContextFilterCategoryButton categoryButton;
    private LogContextFilterValueButton valueButton;

    public CachedLogMenu(@NotNull String title, String key, String value, @Nullable String actionFilter, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        super(9, 5, false, new Structure(
                "# # # # # # # C V",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# x x x x x x x #",
                "# # # < - > # # R")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', Menu.BACKGROUND_ITEM)
                .addIngredient('<', new PreviousButton())
                .addIngredient('-', new BackButton(previous))
                .addIngredient('>', new ForwardButton())
                .addIngredient('R', new RefreshButton())
                .addIngredient('C', new LogContextFilterCategoryButton())
                .addIngredient('V', new LogContextFilterValueButton()
                )
        );

        if (getItem(8, 4) instanceof RefreshButton refreshButton) {
            refreshButton.setRefresh(this::refresh);
        }

        if (getItem(7, 0) instanceof LogContextFilterCategoryButton logContextFilterCategoryButton) {
            this.categoryButton = logContextFilterCategoryButton;
            logContextFilterCategoryButton.setRefresh(this::refresh);
        }

        if (getItem(8, 0) instanceof LogContextFilterValueButton logContextFilterValueButton) {
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
        valueButton.setSelectedContext(categoryButton.getSelectedContext());
        valueButton.getContextValues().clear();
        UtilServer.runTaskAsync(plugin, () -> {
            List<CachedLog> logs = logRepository.getLogsWithContextAndAction(key, value, actionFilter);
            logs.forEach(cachedLog -> {
                cachedLog.getContext().forEach((key, value) -> {
                    valueButton.addValue(key, value);
                });
            });
            List<Item> items = logs.stream()
                    .filter(cachedLog -> {
                        String context = categoryButton.getSelectedContext();
                        if (context == "All") {
                            return true;
                        }
                        if (!cachedLog.getContext().containsKey(context)) {
                            return false;
                        }
                        return (cachedLog.getContext().get(context).equals(valueButton.getSelected()));
                    })
                    .map(cachedLog -> new CachedLogButton(cachedLog, logRepository, this))
                    .map(Item.class::cast).toList();
            setContent(items);
            future.complete(true);
        });
        return future;
    }

    @Override
    public void setContent(@Nullable List<Item> content) {
        super.setContent(content);
    }

    @Override
    public Component getTitle() {
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
