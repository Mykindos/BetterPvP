package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.framework.ClansServerType;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Shows only the servers belonging to a specific {@link ClansServerType}, centered in a single row.
 * Opened by clicking an {@link me.mykindos.betterpvp.hub.feature.npc.InstanceSelectorNPC}.
 */
public class ServerTypeMenu extends AbstractGui implements Windowed {

    private final ClansServerType serverType;

    public ServerTypeMenu(NetworkPlayerCountService networkService, ClansServerType serverType) {
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

        final int[] slots = ServerSelectorMenu.computeSlots(servers.size(), 9);
        for (int i = 0; i < servers.size() && i < slots.length; i++) {
            setItem(slots[i], new ServerItemButton(servers.get(i), networkService, serverType));
        }
    }

    @NotNull
    @Override
    public Component getTitle() {
        return serverType.getDisplayTitle();
    }
}
