package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.logging.menu.button.type.IStringFilterValueButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class StringFilterValueButton<G extends Gui> extends ControlItem<G> implements IStringFilterValueButton {
    @Setter
    private boolean isStatic;
    @Getter
    private final HashMap<String, List<String>> contextValues = new HashMap<>();
    @Setter
    private Supplier<CompletableFuture<Boolean>> refresh;
    private String selectedContext;
    private int selectedValue;

    private final int pageLength;

    public StringFilterValueButton(int pageLength) {
        selectedValue = 0;
        this.pageLength = pageLength;
    }

    private void setSelected(int selected) {
        this.selectedValue = selected;
        refresh.get().whenCompleteAsync(((aBoolean, throwable) -> {
            if (aBoolean.equals(Boolean.TRUE)) {
                this.notifyWindows();
            }
        }));
    }

    private void increase() {
        if (contextValues.get(selectedContext) == null) {
            return;
        }
        int newSelected = selectedValue + 1;
        if (newSelected >= contextValues.get(selectedContext).size()) {
            newSelected = 0;
        }
        setSelected(newSelected);
    }

    private void decrease() {
        if (contextValues.get(selectedContext) == null) {
            return;
        }
        int newSelected = selectedValue - 1;
        if (newSelected < 0) {
            newSelected = contextValues.get(selectedContext).size() - 1;
        }
        setSelected(newSelected);
    }

    @Override
    public void addValue(String context, String value) {
        contextValues.computeIfAbsent(context, k -> new ArrayList<>());
        if (contextValues.get(context).contains(value)) {
            return;
        }
        contextValues.get(context).add(value);
        contextValues.get(context).sort(String::compareToIgnoreCase);
    }

    @Override
    public @Nullable String getSelected() {
        if (contextValues.get(selectedContext) == null) {
            return null;
        }
        return contextValues.get(selectedContext).get(selectedValue);
    }

    @Override
    public void setSelectedContext(String newContext) {
        if (this.selectedContext != null && !selectedContext.equals(newContext)) {
            selectedValue = 0;
        }
        this.selectedContext = newContext;
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
        List<String> values = contextValues.get(selectedContext);
        if (values != null) {
            //put the selected value in the middle, cannot be less than 0
            int min = Math.max(0, selectedValue - pageLength/2);
            //dont scroll down if there is no more values to show
            if (min + pageLength >= contextValues.get(selectedContext).size()) {
                min = Math.max(0, contextValues.get(selectedContext).size() - pageLength);
            }
            //get the max element in this view, that we are showing
            int max = Math.min(min + pageLength, contextValues.get(selectedContext).size());

            //add it to lore
            for (int i = min; i < max; i++) {
                if (i == selectedValue) {
                    lore.add(Component.text(values.get(i) + " \u00AB", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(values.get(i), NamedTextColor.GRAY));
            }
        }


        return ItemView.builder()
                .displayName(Component.text("Select Value", NamedTextColor.WHITE, TextDecoration.BOLD))
                .material(Material.PAPER)
                .lore(lore)
                .build();
    }
}
