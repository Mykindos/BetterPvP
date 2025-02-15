package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class StringFilterButton<G extends Gui> extends ControlItem<G> implements IStringFilterButton {

    @Setter
    private boolean isStatic;

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
    private final Material displayMaterial;
    private final int customModelData;

    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     * @param title the title of this button
     * @param numToShow the number of options to show at once
     * @param displayMaterial the material of the item
     * @param customModelData the custom model data of the item
     */
    public StringFilterButton(String title, int numToShow, Material displayMaterial, int customModelData) {
        this(title, new ArrayList<>(List.of("All")), numToShow, displayMaterial, customModelData);
    }

    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     * @param title the title of this button
     * @param contexts the list of contexts to use
     * @param numToShow the number of options to show at once
     * @param displayMaterial the material of the item
     * @param customModelData the custom model data of the item
     */
    public StringFilterButton(String title, List<String> contexts, int numToShow, Material displayMaterial, int customModelData) {
        this.title = title;
        this.contexts = new ArrayList<>(contexts);
        this.numToShow = numToShow;
        this.displayMaterial = displayMaterial;
        this.customModelData = customModelData;
        this.selected = 0;
        this.selectedFilter = contexts.get(0);

        this.isStatic = false;
    }

    @Override
    public void add(@NotNull String newFilter) {
        if (contexts.contains(newFilter)) {
            return;
        }
        contexts.add(newFilter);
        contexts.sort(String::compareToIgnoreCase);
    }

    public void setSelected(int selected) {
        this.selected = selected;
        this.selectedFilter = contexts.get(selected);
        if (refresh == null) return;
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
        if (isStatic) return;
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
                    lore.add(Component.text(contexts.get(i) + " \u00AB", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(contexts.get(i), NamedTextColor.GRAY));
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
