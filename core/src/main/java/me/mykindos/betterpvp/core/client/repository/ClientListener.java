package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientFetchExternalDataEvent;
import me.mykindos.betterpvp.core.client.events.ClientIgnoreStatusEvent;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.events.ClientUnloadEvent;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(final ServerLoadEvent event) {
        // Loading all clients that are in the server while loading
        Bukkit.getOnlinePlayers().forEach(player -> clientManager.loadOnline(player.getUniqueId(), player.getName()).get().ifPresent(client -> {
            client.setOnline(true);
            Bukkit.getPluginManager().callEvent(new ClientJoinEvent(client, player));
        }));

        this.serverLoaded = true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        checkUnsetProperties(client);

        final ClientJoinEvent joinEvent = new ClientJoinEvent(client, player);
        Bukkit.getPluginManager().callEvent(joinEvent); // Call event after client is loaded
        event.joinMessage(joinEvent.getJoinMessage());

        if (client.hasRank(Rank.ADMIN)) {
            player.setOp(true);
        }

        if (!player.getName().equalsIgnoreCase(client.getName())) {
            clientManager.getSqlLayer().updateClientName(client, player.getName());
            client.setName(player.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
        Optional<Client> client = this.clientManager.loadOnline(
                event.getUniqueId(),
                event.getName()
        ).get();

        if (client.isEmpty()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(ClientManager.LOAD_ERROR_FORMAT_ENTITY));
            return;
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {

        final Client client = clientManager.search().online(event.getPlayer());
        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL || event.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {

            if (client.hasRank(Rank.TRIAL_MOD)) {
                event.allow();
                return;
            }
        }

        if (event.getResult() == PlayerLoginEvent.Result.KICK_BANNED) {
            if (client.hasRank(Rank.DEVELOPER)) {
                event.allow();
            }
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            if (Bukkit.getOnlinePlayers().size() >= maxPlayers && !client.hasRank(Rank.TRIAL_MOD)) {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, Component.text("The server is full!"));
                return;
            }
        }

        String hostAddress = event.getAddress().getHostAddress();
        String saltedAddress = UtilFormat.hashWithSalt(hostAddress, salt);
        log.info("{} ({}) logged in", event.getPlayer().getName(), event.getPlayer().getUniqueId())
                .setAction("CLIENT_LOGIN")
                .addClientContext(event.getPlayer()).addContext("Address", saltedAddress)
                .submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClientLogin(ClientJoinEvent event) {
        if (enableOldPvP) {
            AttributeInstance attribute = event.getPlayer().getAttribute(Attribute.ATTACK_SPEED);
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

    @EventHandler
    public void onIgnoreCheck(ClientIgnoreStatusEvent event) {
        Client client = event.getClient();
        Client target = event.getTarget();
        if (target.hasRank(Rank.HELPER)) {
            return;
        }

        event.setResult(client.getIgnores().contains(target.getUniqueId()) ? ClientIgnoreStatusEvent.Result.DENY : ClientIgnoreStatusEvent.Result.ALLOW);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClientQuit(ClientQuitEvent event) {
        Client client = event.getClient();
        client.putProperty(ClientProperty.TIME_PLAYED, (long) client.getProperty(ClientProperty.TIME_PLAYED).orElse(0L)
                + (System.currentTimeMillis() - client.getConnectionTime()));
        client.getGamer().putProperty(GamerProperty.TIME_PLAYED, (long) client.getGamer().getProperty(GamerProperty.TIME_PLAYED).orElse(0L)
                + (System.currentTimeMillis() - client.getConnectionTime()));
        client.putProperty(ClientProperty.LAST_LOGIN, System.currentTimeMillis());
        client.setConnectionTime(System.currentTimeMillis());
        log.info("{} ({}) quit", event.getPlayer().getName(), event.getPlayer().getUniqueId()).submit();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLunarEvent(LunarClientEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        client.putProperty(ClientProperty.LUNAR, event.isRegistered());
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
        if (sidebarOptional.isEmpty()) {
            client.saveProperty(ClientProperty.SIDEBAR_ENABLED, true);
        }

        Optional<Boolean> tipsOptional = client.getProperty(ClientProperty.TIPS_ENABLED);
        if (tipsOptional.isEmpty()) {
            client.saveProperty(ClientProperty.TIPS_ENABLED, true);
        }

        Optional<Boolean> dropOptional = client.getProperty(ClientProperty.DROP_PROTECTION_ENABLED);
        if (dropOptional.isEmpty()) {
            client.saveProperty(ClientProperty.DROP_PROTECTION_ENABLED, true);
        }

        Optional<Boolean> mapPoiOptional = client.getProperty(ClientProperty.MAP_POINTS_OF_INTEREST);
        if (mapPoiOptional.isEmpty()) {
            client.saveProperty(ClientProperty.MAP_POINTS_OF_INTEREST, true);
        }

        Optional<Boolean> mapPlayerCaptionOptional = client.getProperty(ClientProperty.MAP_PLAYER_NAMES);
        if (mapPlayerCaptionOptional.isEmpty()) {
            client.saveProperty(ClientProperty.MAP_PLAYER_NAMES, false);
        }

        Optional<Boolean> cooldownSoundOptional = client.getProperty(ClientProperty.COOLDOWN_SOUNDS_ENABLED);
        if (cooldownSoundOptional.isEmpty()) {
            client.saveProperty(ClientProperty.COOLDOWN_SOUNDS_ENABLED, true);
        }

        Optional<Boolean> territoryPopupOptional = client.getProperty(ClientProperty.TERRITORY_POPUPS_ENABLED);
        if (territoryPopupOptional.isEmpty()) {
            client.saveProperty(ClientProperty.TERRITORY_POPUPS_ENABLED, true);
        }

        Optional<Boolean> dungeonInviteAlliesOptional = client.getProperty(ClientProperty.DUNGEON_INCLUDE_ALLIES);
        if (dungeonInviteAlliesOptional.isEmpty()) {
            client.saveProperty(ClientProperty.DUNGEON_INCLUDE_ALLIES, false);
        }

        Optional<String> mediaChannelOptional = client.getProperty(ClientProperty.MEDIA_CHANNEL);
        if (mediaChannelOptional.isEmpty()) {
            client.saveProperty(ClientProperty.MEDIA_CHANNEL, "");
        }

        Optional<String> showTagOptional = client.getProperty(ClientProperty.SHOW_TAG);
        if (showTagOptional.isEmpty()) {
            client.saveProperty(ClientProperty.SHOW_TAG, Rank.ShowTag.SHORT.name());
        }

        Optional<Long> lastLoginOptional = client.getProperty(ClientProperty.LAST_LOGIN);
        if (lastLoginOptional.isEmpty()) {
            client.saveProperty(ClientProperty.LAST_LOGIN, 0L);
        }

    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Client client = clientManager.search().online(player);

                ClientFetchExternalDataEvent clientFetchExternalDataEvent = UtilServer.callEvent(new ClientFetchExternalDataEvent(client));
                if (clientFetchExternalDataEvent.getData().isEmpty()) continue;

                clientFetchExternalDataEvent.getData().forEach(client::saveProperty);
            }

        } catch (Exception e) {
            log.error("Error fetching external data", e).submit();
        } finally {
            try {
                this.clientManager.processStatUpdates(true);
            } catch (Exception ex) {
                log.error("Error processing stat updates", ex).submit();
                if (ex.getCause() != null) {
                    log.error("Cause: ", ex.getCause()).submit();
                }
            }
        }

    }

    @EventHandler
    public void onClientUnload(ClientUnloadEvent event) {
        log.info("{} ({}) was unloaded from the cache", event.getClient().getName(), event.getClient().getUuid()).submit();
    }

}
