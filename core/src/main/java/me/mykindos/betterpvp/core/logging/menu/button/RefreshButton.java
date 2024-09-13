package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RefreshButton<G extends Gui> extends ControlItem<G> implements IRefreshButton{

    @Setter
    private Supplier<CompletableFuture<Boolean>> refresh;
    private boolean isRefreshing = false;

    public RefreshButton() {
        this.refresh = null;
    }

    public RefreshButton(Supplier<CompletableFuture<Boolean>> reload) {
        this.refresh = reload;
    }

    @Override
    public ItemProvider getItemProvider(G gui) {
        ItemView.ItemViewBuilder itemViewBuilder = ItemView.builder();
        if (isRefreshing) {
            itemViewBuilder
                    .displayName(Component.text("Reloading...", NamedTextColor.RED))
                    .material(Material.REDSTONE_BLOCK);
        } else {
            itemViewBuilder
                    .displayName(Component.text("Reload", NamedTextColor.GREEN))
                    .lore(Component.text("click to reload"))
                    .material(Material.EMERALD_BLOCK);
        }

        return itemViewBuilder.build();
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
        this.isRefreshing = true;
        CompletableFuture<Boolean> future = refresh.get().whenCompleteAsync(((aBoolean, throwable) -> {
            if (aBoolean.equals(Boolean.TRUE)) {
                this.isRefreshing = false;
                this.notifyWindows();
            }
        }));
        if (!future.isDone()) {
            this.notifyWindows();
        }
    }
}
