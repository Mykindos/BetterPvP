package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@BPvPListener
@CustomLog
@Singleton
public class ClientListener implements Listener {

    private static final String LOADING_CLIENT_FORMAT = "Loading client... {}";
    private static final String SERVER_STILL_LOADING_ERROR = "The server is still starting!";

    @Inject
    @Config(path = "pvp.enableOldPvP", defaultValue = "true")
    private boolean enableOldPvP;

    @Inject
    @Config(path = "server.unlimitedPlayers", defaultValue = "false")
    public boolean unlimitedPlayers;

    @Inject
    @Config(path = "server.maxPlayers", defaultValue = "100")
    public int maxPlayers;

    @Inject
    @Config(path = "core.salt", defaultValue = "")
    private String salt;
    
    private final ClientManager clientManager;

    private boolean serverLoaded;
    private final Set<UUID> usersLoading = Collections.synchronizedSet(new HashSet<>());

    @Inject
    public ClientListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onServerLoad(final ServerLoadEvent event) {
        // Loading all clients that are in the server while loading
        Bukkit.getOnlinePlayers().forEach(player -> clientManager.loadOnline(player.getUniqueId(), player.getName(), success -> {
            // Call event after a client is loaded
            Bukkit.getPluginManager().callEvent(new ClientJoinEvent(success, player));
        }, null));

        this.serverLoaded = true;
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        checkUnsetProperties(client);

        final ClientJoinEvent joinEvent = new ClientJoinEvent(client, player);
        Bukkit.getPluginManager().callEvent(joinEvent); // Call event after client is loaded
        event.joinMessage(joinEvent.getJoinMessage());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onLeave(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Optional<Client> clientOpt = clientManager.getStoredExact(player.getUniqueId());
        clientOpt.ifPresent(client -> {
            CompletableFuture.runAsync(() -> {
                // Removing the client as they logged off
                this.clientManager.unload(client);
            });

            final ClientQuitEvent quitEvent = new ClientQuitEvent(client, player);
            UtilServer.callEvent(quitEvent);
            event.quitMessage(quitEvent.getQuitMessage());
            client.setOnline(false);
        });
    }

    @EventHandler
    public void onLoad(final AsyncPlayerPreLoginEvent event) throws InterruptedException {
        if (!this.serverLoaded) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(SERVER_STILL_LOADING_ERROR));
            return;
        }

        this.usersLoading.add(event.getUniqueId());

        log.info(LOADING_CLIENT_FORMAT, event.getName()).submit();
        this.clientManager.loadOnline(
                event.getUniqueId(),
                event.getName(),
                client -> this.usersLoading.remove(event.getUniqueId()),
                () -> {
                    this.usersLoading.remove(event.getUniqueId());
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(ClientManager.LOAD_ERROR_FORMAT_ENTITY));
                }
        );

        int waitMillis = 0;
        while (this.usersLoading.contains(event.getUniqueId()) && waitMillis <= 10 * 1000) {
            //noinspection BusyWait
            Thread.sleep(2);
            waitMillis += 2;
        }

        if (this.usersLoading.contains(event.getUniqueId())) {
            this.usersLoading.remove(event.getUniqueId());
            log.warn(ClientManager.LOAD_ERROR_FORMAT_SERVER, event.getName()).submit();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(ClientManager.LOAD_ERROR_FORMAT_ENTITY));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (unlimitedPlayers && event.getResult() == PlayerLoginEvent.Result.KICK_FULL){
            event.allow();
            return;
        }
        final Client client = clientManager.search().online(event.getPlayer());
        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL || event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {

            if (client.hasRank(Rank.TRIAL_MOD)) {
                event.allow();
                return;
            }
        }

        if(event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            if (Bukkit.getOnlinePlayers().size() >= maxPlayers && !client.hasRank(Rank.TRIAL_MOD)) {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, Component.text("The server is full!!"));
                return;
            }
        }

        String hostAddress = event.getAddress().getHostAddress();
        String saltedAddress = UtilFormat.hashWithSalt(hostAddress, salt);
        log.info("{} ({}) logged in", event.getPlayer().getName(), event.getPlayer().getUniqueId())
                .setAction("CLIENT_LOGIN")
                .addClientContext(event.getPlayer()).addContext("Address", saltedAddress)
                .submit();

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            var alts = clientManager.getSqlLayer().getAlts(event.getPlayer(), saltedAddress);
            if(!alts.isEmpty()) {
                String altString = String.join(", ", alts);
                clientManager.sendMessageToRank("Client", UtilMessage.deserialize("<red>%s<reset> is an alt of <red>%s", event.getPlayer().getName(), altString), Rank.ADMIN);
            }
        });
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onClientLogin(ClientJoinEvent event) {
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

        event.getClient().setConnectionTime(System.currentTimeMillis());
        log.info("{} ({}) joined", event.getPlayer().getName(), event.getPlayer().getUniqueId()).submit();
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClientQuit(ClientQuitEvent event) {
        Client client = event.getClient();
        client.putProperty(ClientProperty.TIME_PLAYED, (long) client.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)
                + (System.currentTimeMillis() - client.getConnectionTime()));
        client.getGamer().putProperty(GamerProperty.TIME_PLAYED, (long) client.getGamer().getProperty(GamerProperty.TIME_PLAYED).orElse(0L)
                + (System.currentTimeMillis() - client.getConnectionTime()));
        client.setConnectionTime(System.currentTimeMillis());
        log.info("{} ({}) quit", event.getPlayer().getName(), event.getPlayer().getUniqueId()).submit();
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onLunarEvent(LunarClientEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        client.putProperty(ClientProperty.LUNAR, event.isRegistered());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onSettingsUpdated(ClientPropertyUpdateEvent event) {
        this.clientManager.saveProperty(event.getClient(), event.getProperty(), event.getValue());
    }

    private void checkUnsetProperties(Client client) {

        ClientProperty chatEnabledProperty = ClientProperty.CHAT_ENABLED;
        Optional<Integer> chatEnabledOptional = client.getProperty(chatEnabledProperty);
        if (chatEnabledOptional.isEmpty()) {
            client.saveProperty(chatEnabledProperty, true);
        }

        Optional<Boolean> sidebarOptional = client.getProperty(ClientProperty.SIDEBAR_ENABLED);
        if(sidebarOptional.isEmpty()){
            client.saveProperty(ClientProperty.SIDEBAR_ENABLED, true);
        }

        Optional<Boolean> tipsOptional = client.getProperty(ClientProperty.TIPS_ENABLED);
        if(tipsOptional.isEmpty()){
            client.saveProperty(ClientProperty.TIPS_ENABLED, true);
        }

        Optional<Boolean> dropOptional = client.getProperty(ClientProperty.DROP_PROTECTION_ENABLED);
        if(dropOptional.isEmpty()){
            client.saveProperty(ClientProperty.DROP_PROTECTION_ENABLED, true);
        }

        Optional<Boolean> mapPoiOptional = client.getProperty(ClientProperty.MAP_POINTS_OF_INTEREST);
        if(mapPoiOptional.isEmpty()){
            client.saveProperty(ClientProperty.MAP_POINTS_OF_INTEREST, true);
        }

        Optional<Boolean> mapPlayerCaptionOptional = client.getProperty(ClientProperty.MAP_PLAYER_NAMES);
        if(mapPlayerCaptionOptional.isEmpty()){
            client.saveProperty(ClientProperty.MAP_PLAYER_NAMES, false);
        }
    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates() {
        this.clientManager.processStatUpdates(true);
    }



}
