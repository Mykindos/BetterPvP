package me.mykindos.betterpvp.hub.feature.menu;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.ClansServerType;
import me.mykindos.betterpvp.core.framework.ServerType;
import me.mykindos.betterpvp.core.framework.server.network.NetworkPlayerCountService;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.orchestration.transport.QueuePluginChannels;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ServerItemButton extends AbstractItem {

    private final String serverName;
    private final ServerType serverType;
    private final NetworkPlayerCountService networkService;

    public ServerItemButton(String serverName, NetworkPlayerCountService networkService, ServerType serverType) {
        this.serverName = serverName;
        this.networkService = networkService;
        this.serverType = serverType;
    }

    @Override
    public ItemProvider getItemProvider() {
        final Map<String, Integer> counts = networkService.getServerPlayerCounts();
        final boolean online = counts.containsKey(serverName);
        final String displayName = formatServerName(serverName);

        ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(online ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                .displayName(Component.text(displayName, online ? NamedTextColor.GREEN : NamedTextColor.RED))
                .lore(Component.empty());

        if (serverType instanceof ClansServerType clansServerType) {
            final int squadSize = clansServerType.getSquadSize();
            builder = builder.lore(Component.text("Clan Size: ", NamedTextColor.GRAY).append(Component.text(squadSize, NamedTextColor.YELLOW)));
        }

        if (online) {
            final int playerCount = counts.getOrDefault(serverName, 0);
            builder = builder.lore(Component.text("Players: ", NamedTextColor.GRAY).append(Component.text(playerCount, NamedTextColor.GREEN)));
        }

        builder = builder.lore(Component.empty());
        return builder.build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final Map<String, Integer> counts = networkService.getServerPlayerCounts();
        if (!counts.containsKey(serverName)) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(serverName);
            player.sendPluginMessage(JavaPlugin.getPlugin(Core.class), QueuePluginChannels.QUEUE_REQUEST, bytes.toByteArray());
        } catch (IOException ignored) {
            SoundEffect.WRONG_ACTION.play(player);
        }
    }

    private static String formatServerName(String raw) {
        final String[] parts = raw.split("-");
        final StringBuilder sb = new StringBuilder();
        for (final String part : parts) {
            if (!sb.isEmpty()) {
                sb.append('-');
            }
            if (!part.isEmpty()) {
                final String lower = part.toLowerCase(Locale.ROOT);
                sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return sb.toString();
    }
}
