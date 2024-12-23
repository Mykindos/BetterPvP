
package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public class UUIDItemButton extends LogRepositoryButton {
    private final String uuid;

    public UUIDItemButton(String name, String uuid, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        super(name, LogContext.ITEM, uuid, "ITEM_", CachedLogMenu.ITEM, plugin, logRepository, previous);
        this.uuid = uuid;
    }

    @Override
    public ItemProvider getItemProvider() {
        List<Component> lore = List.of(
                Component.text(name),
                Component.text("Click to search by UUID")
        );
        return ItemView.builder()
                .displayName(Component.text(uuid))
                .material(Material.IRON_SWORD)
                .customModelData(0)
                .lore(lore)
                .frameLore(true)
                .build();
    }
}