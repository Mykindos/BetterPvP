package me.mykindos.betterpvp.core.world.menu.button;

import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;

import java.util.function.Consumer;

public class FolderButton extends AbstractItem {

    private final String folderName;
    private final Consumer<Player> onClick;

    public FolderButton(String folderName, Consumer<Player> onClick) {
        this.folderName = folderName;
        this.onClick = onClick;
    }

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder()
                .material(Material.CHEST)
                .displayName(Component.text(folderName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(Component.text("Click to open folder", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        onClick.accept(player);
    }
}
