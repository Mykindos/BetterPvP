package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.ClansServerType;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ServerSelectorMenu extends AbstractGui implements Windowed {

    public ServerSelectorMenu(NetworkPlayerCountService networkService, HubQueueStatusRegistry queueStatusRegistry,
                              OrchestrationGateway orchestrationGateway) {
        super(9, 3);
        fill(Menu.BACKGROUND_GUI_ITEM, true);

        final List<Map.Entry<String, ClansServerType>> servers = networkService.getServerPlayerCounts().keySet().stream()
                .sorted()
                .map(serverName -> {
                    final ClansServerType serverType = resolveType(serverName);
                    return serverType == null ? null : Map.entry(serverName, serverType);
                })
                .filter(Objects::nonNull)
                .toList();

        final int[] slots = computeSlots(servers.size(), 9);
        for (int i = 0; i < slots.length; i++) {
            final Map.Entry<String, ClansServerType> entry = servers.get(i);
            setItem(slots[i], new ServerItemButton(entry.getKey(), networkService, entry.getValue(), queueStatusRegistry, orchestrationGateway));
        }
    }

    static int[] computeSlots(int n, int rowOffset) {
        if (n <= 0) {
            return new int[0];
        }

        final int itemCount = Math.min(n, 9);
        final int start = rowOffset + (9 - itemCount) / 2;
        final int[] slots = new int[itemCount];
        for (int i = 0; i < itemCount; i++) {
            slots[i] = start + i;
        }
        return slots;
    }

    private static ClansServerType resolveType(String serverName) {
        for (ClansServerType serverType : List.of(ServerTypes.CLANS_CLASSIC, ServerTypes.CLANS_SQUADS, ServerTypes.CLANS_CASUAL)) {
            if (serverName.startsWith(serverType.getServerNamePrefix())) {
                return serverType;
            }
        }

        return null;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Server Select", NamedTextColor.DARK_GRAY);
    }

}
