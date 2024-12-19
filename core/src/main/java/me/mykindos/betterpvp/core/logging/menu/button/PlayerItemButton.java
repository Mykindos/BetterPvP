
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlayerItemButton extends LogRepositoryButton {
    private final String uuid;
    private final String relation;

    public PlayerItemButton(String name, @Nullable String uuid, String relation, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        super(name, LogContext.CLIENT, uuid, "ITEM_", CachedLogMenu.ITEM, plugin, logRepository, previous);
        this.uuid = uuid;
        this.relation = relation;
    }

    @Override
    public ItemProvider getItemProvider() {
        if (uuid == null) {
            return ItemView.builder()
                    .displayName(Component.text("NULL"))
                    .lore(Component.text("No Player For This Log"))
                    .material(Material.SKELETON_SKULL)
                    .build();
        }

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