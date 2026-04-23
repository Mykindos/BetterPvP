package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.SelectableServerType;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Shows only the servers belonging to a specific {@link SelectableServerType}, centered in a single row.
 * Opened by clicking an {@link me.mykindos.betterpvp.hub.feature.npc.InstanceSelectorNPC}.
 */
public class ServerTypeMenu extends AbstractGui implements Windowed {

    private final SelectableServerType serverType;

    public ServerTypeMenu(NetworkPlayerCountService networkService, SelectableServerType serverType,
                          HubQueueStatusRegistry queueStatusRegistry, OrchestrationGateway orchestrationGateway) {
        super(9, 3);
        this.serverType = serverType;

        // Background fills everything first
        for (int i = 0; i < 27; i++) {
            setItem(i, Menu.BACKGROUND_GUI_ITEM);
        }

        // Filter to servers matching this type's name prefix, sorted alphabetically
        final Map<String, Integer> counts = networkService.getServerPlayerCounts();
        final List<String> servers = counts.keySet().stream()
                .filter(name -> name.startsWith(serverType.getServerNamePrefix()))
                .sorted()
                .toList();

        final int[] slots = computeSlots(servers.size(), 9);
        for (int i = 0; i < servers.size() && i < slots.length; i++) {
            setItem(slots[i], new ServerItemButton(servers.get(i), networkService, serverType, queueStatusRegistry, orchestrationGateway));
        }
    }

    private int[] computeSlots(int n, int rowOffset) {
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

    @NotNull
    @Override
    public Component getTitle() {
        return serverType.getDisplayTitle();
    }
}
