package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.SelectableServerType;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class HubServersButton extends AbstractItem {

    private final SelectableServerType serverType;
    private final NetworkPlayerCountService networkService;
    private final HubQueueStatusRegistry queueStatusRegistry;
    private final OrchestrationGateway orchestrationGateway;

    public HubServersButton(NetworkPlayerCountService networkService, SelectableServerType serverType,
                            HubQueueStatusRegistry queueStatusRegistry, OrchestrationGateway orchestrationGateway) {
        this.networkService = networkService;
        this.serverType = serverType;
        this.queueStatusRegistry = queueStatusRegistry;
        this.orchestrationGateway = orchestrationGateway;
    }

    @Override
    public ItemProvider getItemProvider() {
        return ItemView.builder().material(Material.CLOCK)
                .displayName(Component.text("Hub", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new ServerTypeMenu(networkService, serverType, queueStatusRegistry, orchestrationGateway).show(player);
    }

}
