package me.mykindos.betterpvp.core.menu.button.filter;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


/**
 * @param <G> the gui
 * @param <T> the type of the context
 */
public class FilterButton<G extends Gui, T extends IFilterContext<T>> extends ControlItem<G> implements IContextFilterButton<T> {


    @Getter
    private final List<T> contexts;
    @Setter
    @Nullable
    private Supplier<CompletableFuture<Boolean>> refresh;
    @Getter
    private T selectedFilter;
    private int selected;
    private final int numToShow;
    private final String title;
    private final Material displayMaterial;
    private final int customModelData;

    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     * @param title the title of this button
     * @param contexts the list of contexts to use
     * @param numToShow the number of options to show at once
     * @param displayMaterial the material of the item
     * @param customModelData the custom model data of the item
     */
    public FilterButton(String title, List<T> contexts, int numToShow, Material displayMaterial, int customModelData) {
        this.title = title;
        this.contexts = new ArrayList<>(contexts);
        this.numToShow = numToShow;
        this.displayMaterial = displayMaterial;
        this.customModelData = customModelData;
        this.selected = 0;
        this.selectedFilter = contexts.getFirst();
        this.contexts.sort(null);
    }

    @Override
    public void add(@NotNull T newFilter) {
        if (contexts.contains(newFilter)) {
            return;
        }
        contexts.add(newFilter);
        contexts.sort(null);
    }

    public void setSelectedFilter(T filter) {
        if (filter == null) return;
        int newSelected = contexts.indexOf(filter);
        if (newSelected < 0) {
            throw new NoSuchElementException("Unknown element " + filter);
        }
        setSelected(newSelected);
    }

    private void setSelected(int selected) {
        this.selected = selected;
        this.selectedFilter = contexts.get(selected);
        if (refresh != null) {
            refresh.get().whenCompleteAsync(((aBoolean, throwable) -> {
                if (aBoolean.equals(Boolean.TRUE)) {
                    this.notifyWindows();
                    if (getGui() instanceof AbstractGui abstractGui) {
                        abstractGui.updateControlItems();
                    }
                }
            }));
            return;
        }
        this.notifyWindows();
        if (getGui() instanceof AbstractGui abstractGui) {
            abstractGui.updateControlItems();
        }
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
    public ItemProvider getItemProvider(G gui) {
        List<Component> lore = new ArrayList<>();
        if (!contexts.isEmpty()) {
            //put the selected value in the middle, cannot be less than 0
            int min = Math.max(0, selected - numToShow/2);
            //dont scroll down if there is no more values to show
            if (min + numToShow >= contexts.size()) {
                min = Math.max(0, contexts.size() - numToShow);
            }
            //get the max element in this view, that we are showing
            int max = Math.min(min + numToShow, contexts.size());

            //add it to lore
            for (int i = min; i < max; i++) {
                if (i == selected) {
                    lore.add(Component.text(contexts.get(i).getDisplay() + " \u00AB", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(contexts.get(i).getDisplay(), NamedTextColor.GRAY));
            }
        }


        return ItemView.builder()
                .displayName(Component.text(title, NamedTextColor.WHITE, TextDecoration.BOLD))
                .material(displayMaterial)
                .customModelData(customModelData)
                .lore(lore)
                .build();
    }
}
