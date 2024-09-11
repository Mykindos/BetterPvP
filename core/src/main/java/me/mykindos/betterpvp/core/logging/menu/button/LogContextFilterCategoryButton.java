package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class LogContextFilterCategoryButton extends ControlItem<CachedLogMenu> {


    @Getter
    private final String[] contexts;
    @Setter
    private Supplier<CompletableFuture<Boolean>> refresh;
    @Setter
    @Getter
    private String selectedContext;
    private int selected;

    public LogContextFilterCategoryButton() {
        contexts = new String[]{
                "All",
                LogContext.CLIENT,
                LogContext.CLIENT_NAME,
                LogContext.TARGET_CLIENT,
                LogContext.TARGET_CLIENT_NAME,
                LogContext.CLAN,
                LogContext.CLAN_NAME,
                LogContext.TARGET_CLAN,
                LogContext.TARGET_CLAN_NAME
        };
        this.selected = 0;
        this.selectedContext = "All";
    }

    private void setSelected(int selected) {
        this.selected = selected;
        this.selectedContext = contexts[selected];
        refresh.get().whenCompleteAsync(((aBoolean, throwable) -> {
            if (aBoolean.equals(Boolean.TRUE)) {
                this.notifyWindows();
            }
        }));
    }

    private void increase() {
        int newSelected = selected + 1;
        if (newSelected >= contexts.length) {
            newSelected = 0;
        }
        setSelected(newSelected);
    }

    private void decrease() {
        int newSelected = selected - 1;
        if (newSelected < 0) {
            newSelected = contexts.length - 1;
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
        List<Component> lore = Arrays.stream(contexts)
                .map(logContext -> {
                    if (logContext == null) {
                        logContext = "All";
                    }
                    if (logContext.equals(selectedContext)) {
                        return Component.text(">" + logContext + "<", NamedTextColor.GREEN);
                    }
                    return Component.text(logContext, NamedTextColor.GRAY);
                })
                .map(TextComponent::asComponent)
                .toList();

        return ItemView.builder()
                .displayName(Component.text("Select Category", NamedTextColor.WHITE))
                .material(Material.ANVIL)
                .lore(lore)
                .build();
    }
}
