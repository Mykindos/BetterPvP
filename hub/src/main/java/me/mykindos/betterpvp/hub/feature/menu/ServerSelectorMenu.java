package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.SelectableServerType;
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

        final List<Map.Entry<String, SelectableServerType>> servers = networkService.getServerPlayerCounts().keySet().stream()
                .sorted()
                .map(serverName -> {
                    final SelectableServerType serverType = resolveType(serverName);
                    return serverType == null ? null : Map.entry(serverName, serverType);
                })
                .filter(Objects::nonNull)
                .toList();

        final int[] slots = computeSlots(servers.size(), 9);
        for (int i = 0; i < slots.length; i++) {
            final Map.Entry<String, SelectableServerType> entry = servers.get(i);
            setItem(slots[i], new ServerItemButton(entry.getKey(), networkService, entry.getValue(), queueStatusRegistry, orchestrationGateway));
        }
    }

    static int[] computeSlots(int n, int rowOffset) {
        if (n <= 0) {
            return new int[0];
        }

        final int count = Math.min(n, 9);
        final int center = rowOffset + 4;
        final int[] slots = new int[count];

        if (count <= 5) {
            // Spaced layout: step-2 between items, centered on the row center.
            // Odd count places an item on center; even count leaves center empty.
            // Formula: center + 2*i - (count-1)  →  start=center-(count-1), step=2
            for (int i = 0; i < count; i++) {
                slots[i] = center + 2 * i - (count - 1);
            }
        } else {
            // Packed layout: items are consecutive, split symmetrically around center.
            // Odd count places an item on center; even count leaves center empty.
            final int half = count / 2;
            for (int i = 0; i < half; i++) {
                slots[i] = center - half + i;           // left side
            }
            if (count % 2 == 1) {
                slots[half] = center;                   // center item (odd only)
                for (int i = 0; i < half; i++) {
                    slots[half + 1 + i] = center + 1 + i; // right side
                }
            } else {
                for (int i = 0; i < half; i++) {
                    slots[half + i] = center + 1 + i;  // right side
                }
            }
        }

        return slots;
    }

    private static SelectableServerType resolveType(String serverName) {
        for (SelectableServerType serverType : List.of(
                ServerTypes.CLANS_CLASSIC, ServerTypes.CLANS_SQUADS, ServerTypes.CLANS_CASUAL,
                ServerTypes.CHAMPIONS)) {
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
