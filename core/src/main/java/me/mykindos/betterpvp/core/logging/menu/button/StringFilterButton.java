package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class StringFilterButton extends ControlItem<CachedLogMenu> {


    @Getter
    private final List<String> contexts;
    @Setter
    private Supplier<CompletableFuture<Boolean>> refresh;
    @Setter
    @Getter
    private String selectedFilter;
    private int selected;
    private final int numToShow;
    private final String title;

    public StringFilterButton(String title, int numToShow) {
        this(title, new ArrayList<>(List.of("All")), numToShow);
    }

    public StringFilterButton(String title, List<String> contexts, int numToShow) {
        this.title = title;
        this.contexts = new ArrayList<>(contexts);
        this.numToShow = numToShow;
        this.selected = 0;
        this.selectedFilter = contexts.get(0);
    }

    public void add(String newFilter) {
        if (contexts.contains(newFilter)) {
            return;
        }
        contexts.add(newFilter);
    }

    private void setSelected(int selected) {
        this.selected = selected;
        this.selectedFilter = contexts.get(selected);
        refresh.get().whenCompleteAsync(((aBoolean, throwable) -> {
            if (aBoolean.equals(Boolean.TRUE)) {
                this.notifyWindows();
            }
        }));
    }

    private void increase() {
        int newSelected = selected + 1;
        if (newSelected >= contexts.size()) {
            newSelected = 0;
        }
        setSelected(newSelected);
    }

    private void decrease() {
        int newSelected = selected - 1;
        if (newSelected < 0) {
            newSelected = contexts.size() - 1;
        }
        setSelected(newSelected);
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isLeftClick()) {
            increase();
        }
        if (clickType.isRightClick()) {
            decrease();
        }
    }

    @Override
    public ItemProvider getItemProvider(CachedLogMenu gui) {
        List<Component> lore = new ArrayList<>();
        if (!contexts.isEmpty()) {
            contexts.sort(String::compareToIgnoreCase);
            int min = Math.max(0, selected - numToShow/2);
            int tempMax = min + numToShow;
            if (tempMax >= contexts.size()) {
                min = Math.max(0, contexts.size() - numToShow);
            }

            int max = Math.min(min + numToShow, contexts.size());
            for (int i = min; i < max; i++) {
                if (i == selected) {
                    lore.add(Component.text(">" + contexts.get(i) + "<", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(contexts.get(i), NamedTextColor.GRAY));
            }
        }


        return ItemView.builder()
                .displayName(Component.text(title, NamedTextColor.WHITE))
                .material(Material.ANVIL)
                .lore(lore)
                .build();
    }
}
