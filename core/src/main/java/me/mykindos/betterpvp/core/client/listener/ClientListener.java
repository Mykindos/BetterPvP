package me.mykindos.betterpvp.core.client.listener;

import com.google.inject.Inject;
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
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

@BPvPListener
public class ClientListener implements Listener {

    @Inject
    @Config(path = "pvp.enableOldPvP", defaultValue = "true")
    private boolean enableOldPvP;

    @Inject
    @Config(path = "tab.title", defaultValue = "Welcome to Mineplex Clans!")
    private String tabTitle;

    @Inject
    @Config(path = "tab.website", defaultValue = "https://mineplex.com")
    private String website;

    @Inject
    @Config(path = "tab.shop", defaultValue = "mineplex.com/shop")
    private String shop;

    @Inject
    @Config(path = "tab.server", defaultValue = "Clans-1")
    private String server;

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
        var header = Component.text("Mineplex ", NamedTextColor.GOLD)
                .append(Component.text("Network ", NamedTextColor.WHITE))
                .append(Component.text(server, NamedTextColor.GREEN));

        var footer = Component.text("Visit ", NamedTextColor.WHITE)
                .append(Component.text(shop, NamedTextColor.YELLOW))
                .append(Component.text(" for cool perks!", NamedTextColor.WHITE));

        player.sendPlayerListHeaderAndFooter(header, footer);
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



}
