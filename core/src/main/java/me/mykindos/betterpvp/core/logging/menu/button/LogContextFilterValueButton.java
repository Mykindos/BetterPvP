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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class LogContextFilterValueButton extends ControlItem<CachedLogMenu> {

    @Getter
    private final HashMap<String, List<String>> contextValues = new HashMap<>();
    @Setter
    private Supplier<CompletableFuture<Boolean>> refresh;
    @Setter
    private String selectedContext;
    private int selectedValue;

    private int pageLength;

    public LogContextFilterValueButton() {
        selectedValue = 0;
        pageLength = 9;
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

    public void addValue(String context, String value) {
        contextValues.computeIfAbsent(context, k -> new ArrayList<>());
        if (contextValues.get(context).contains(value)) {
            return;
        }
        contextValues.get(context).add(value);
    }

    public @Nullable String getSelected() {
        if (contextValues.get(selectedContext) == null) {
            return null;
        }
        return contextValues.get(selectedContext).get(selectedValue);
    }

    public void setSelectedContext(String newContext) {
        if (this.selectedContext != null && !selectedContext.equals(newContext)) {
            selectedValue = 0;
        }
        this.selectedContext = newContext;
        //Bukkit.broadcastMessage("NEW CONTEXT " + this.selectedContext);
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
        List<String> values = contextValues.get(selectedContext);
        if (values != null) {
            values.sort(String::compareToIgnoreCase);
            int min = Math.max(0, selectedValue - pageLength/2);
            int tempMax = min + pageLength;
            if (tempMax >= contextValues.get(selectedContext).size()) {
                min = Math.max(0, contextValues.get(selectedContext).size() - pageLength);
            }
            int max = Math.min(min + pageLength, contextValues.get(selectedContext).size());
            for (int i = min; i < max; i++) {
                if (i == selectedValue) {
                    lore.add(Component.text(">" + values.get(i) + "<", NamedTextColor.GREEN));
                    continue;
                }
                lore.add(Component.text(values.get(i), NamedTextColor.GRAY));
            }
        }


        return ItemView.builder()
                .displayName(Component.text("Select Value", NamedTextColor.WHITE))
                .material(Material.ANVIL)
                .lore(lore)
                .build();
    }
}
