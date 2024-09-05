
package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.Core;
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

public class PlayerItemButton extends AbstractItem {
    private final LogRepository logRepository;
    private final String name;
    private final String uuid;
    private final String relation;
    private final Windowed previous;

    public PlayerItemButton(String name, String uuid, String relation, LogRepository logRepository, Windowed previous) {
        this.logRepository = logRepository;
        this.name = name;
        this.uuid = uuid;
        this.relation = relation;
        this.previous = previous;
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
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            List<CachedLog> logs = logRepository.getLogsWithContextAndAction(LogContext.CLIENT, uuid, "ITEM_");

            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                new CachedLogMenu(name, logs, logRepository, previous).show(player);
            });
        });
    }
}