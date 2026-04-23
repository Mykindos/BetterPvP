package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.ServerTypes;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class ServerSelectorMenu extends AbstractGui implements Windowed {

    public ServerSelectorMenu(NetworkPlayerCountService networkService, HubQueueStatusRegistry queueStatusRegistry,
                              OrchestrationGateway orchestrationGateway) {
        super(9, 3);
        fill(Menu.BACKGROUND_GUI_ITEM, true);

        setItem(11, new ClansServersButton(networkService, ServerTypes.CLANS_CLASSIC, queueStatusRegistry, orchestrationGateway));
        setItem(15, new ChampionsServersButton(networkService, ServerTypes.CHAMPIONS, queueStatusRegistry, orchestrationGateway));
        setItem(22, new HubServersButton(networkService, ServerTypes.HUB, queueStatusRegistry, orchestrationGateway));
    }



    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Server Select", NamedTextColor.DARK_GRAY);
    }

}
