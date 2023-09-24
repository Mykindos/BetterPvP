package me.mykindos.betterpvp.core.client.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.inject.Inject;
import java.util.Optional;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
public class ClientListener implements Listener {

    @Inject
    @Config(path = "pvp.enableOldPvP", defaultValue = "true")
    private boolean enableOldPvP;

    private final ClientManager clientManager;

    @Inject
    public ClientListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        Optional<Client> clientOptional = clientManager.getObject(uuid);
        Client client;
        if (clientOptional.isEmpty()) {
            client = new Client(uuid, event.getPlayer().getName(), Rank.PLAYER);
            clientManager.addObject(uuid, client);
            clientManager.getRepository().save(client);

            event.joinMessage(UtilMessage.deserialize("<green>New> <gray>%s", event.getPlayer().getName()));
        } else {
            event.joinMessage(UtilMessage.deserialize("<green>Login> <gray>%s", event.getPlayer().getName()));
            client = clientOptional.get();
        }

        checkUnsetProperties(client);

        Bukkit.getPluginManager().callEvent(new ClientLoginEvent(client, event.getPlayer()));
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {
        if (enableOldPvP) {
            AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attribute != null) {
                double baseValue = attribute.getBaseValue();

                // Setting this higher than the usual actually force removes the 1.9 attack indicator
                if (baseValue != 100000000) {
                    attribute.setBaseValue(100000000);
                    event.getPlayer().saveData();

                }
            }
        }

        updateTab(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clientManager.getClientByName(event.getPlayer().getName()).ifPresent(client -> {
            ClientQuitEvent quitEvent = UtilServer.callEvent(new ClientQuitEvent(client, event.getPlayer()));
            if (!quitEvent.isCancelled()) {
                event.quitMessage(quitEvent.getQuitMessage());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClientQuit(ClientQuitEvent event) {
        event.setQuitMessage(UtilMessage.deserialize("<red>Leave> <gray>%s", event.getPlayer().getName()));
    }

    @EventHandler
    public void onLunarEvent(LunarClientEvent event) {
        System.out.println(event.getPlayer().getName());
        Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId().toString());
        clientOptional.ifPresent(client -> {
            client.putProperty(ClientProperty.LUNAR, event.isRegistered());
        });
    }


    @UpdateEvent(delay = 5000)
    public void updateTabAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(this::updateTab);
    }

    @SneakyThrows
    public void updateTab(Player player) {

        PacketContainer pc = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER);

        var titleTop = Component.text("Welcome to BetterPvP Clans!\n", NamedTextColor.RED, TextDecoration.BOLD);
        var titleBot = Component.text("Visit our website at: ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("https://betterpvp.net", NamedTextColor.YELLOW, TextDecoration.BOLD));
        var header = titleTop.append(Component.newline()).append(titleBot);

        var footerLeft = Component.text("Ping: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(UtilPlayer.getPing(player), NamedTextColor.YELLOW, TextDecoration.BOLD));
        var footerRight = Component.text("Online: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(Bukkit.getOnlinePlayers().size(), NamedTextColor.YELLOW, TextDecoration.BOLD));
        var footer = footerLeft.append(Component.space()).append(footerRight);

        player.sendPlayerListHeaderAndFooter(header, footer);
//        pc.getChatComponents().write(0, title).write(1, info);
//        ProtocolLibrary.getProtocolManager().sendServerPacket(player, pc);
    }

    @EventHandler
    public void onSettingsUpdated(ClientPropertyUpdateEvent event) {
        clientManager.getRepository().saveProperty(event.getClient(), event.getProperty(), event.getValue());
    }

    private void checkUnsetProperties(Client client) {

        ClientProperty chatEnabledProperty = ClientProperty.CHAT_ENABLED;
        Optional<Integer> chatEnabledOptional = client.getProperty(chatEnabledProperty);
        if (chatEnabledOptional.isEmpty()) {
            client.saveProperty(chatEnabledProperty, true);
        }

    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates() {
        clientManager.getRepository().processStatUpdates(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
    }

}
