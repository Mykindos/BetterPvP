package me.mykindos.betterpvp.hub.feature.sidebar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;

import java.util.Objects;
@Singleton
public class DefaultSidebarBuilder implements HubSidebarBuilder {

    private final ClientManager clientManager;
    private final NetworkPlayerCountService networkPlayerCountService;

    @Inject
    private DefaultSidebarBuilder(ClientManager clientManager, NetworkPlayerCountService networkPlayerCountService) {
        this.clientManager = clientManager;
        this.networkPlayerCountService = networkPlayerCountService;
    }

    @Override
    public void build(SidebarBuildEvent event) {
        final SidebarComponent.Builder builder = event.getBuilder();
        final Player player = Objects.requireNonNull(event.getGamer().getPlayer());
        final Client client = clientManager.search().online(player);

        builder.addBlankLine();
        builder.addStaticLine(Component.text("Rank", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> Component.text(client.getRank().getName(), client.getRank().getColor()));
        builder.addBlankLine();
        builder.addStaticLine(Component.text("Online", NamedTextColor.YELLOW, TextDecoration.BOLD));
        builder.addDynamicLine(() -> Component.text(networkPlayerCountService.getOnlineCount(), NamedTextColor.WHITE));
        builder.addBlankLine();
    }
}
