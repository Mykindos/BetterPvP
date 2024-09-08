
package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerItemButton extends LogRepositoryButton {
    private final String uuid;
    private final String relation;

    public PlayerItemButton(String name, String uuid, String relation, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        super(name, LogContext.CLIENT, uuid, "ITEM_", plugin, logRepository, previous);
        this.uuid = uuid;
        this.relation = relation;
    }

    @Override
    public ItemProvider getItemProvider() {
        List<Component> lore = List.of(
                Component.text(relation),
                Component.text(uuid),
                Component.text("Click to search by Player")
        );
        return ItemView.builder()
                .displayName(Component.text(name))
                .material(Material.PLAYER_HEAD)
                .customModelData(0)
                .lore(lore)
                .frameLore(true)
                .build();
    }
}