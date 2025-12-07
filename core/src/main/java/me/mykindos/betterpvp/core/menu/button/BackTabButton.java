package me.mykindos.betterpvp.core.menu.button;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Button that shows a description and wiki entries.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class BackTabButton extends AbstractItem {

    private final Consumer<Player> onBack;

    public BackTabButton(Runnable onBack) {
        this.onBack = player -> onBack.run();
    }

    public BackTabButton(@Nullable Windowed previous) {
        if (previous == null) {
            this.onBack = HumanEntity::closeInventory;
        } else {
            this.onBack = previous::show;
        }
    }

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/tab/back"))
                .displayName(Component.text("Back", TextColor.color(245, 71, 59)))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        onBack.accept(player);
    }
}
