package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.MapPlayedTimeStat;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class MapStatListener implements Listener {
    private static final long UPDATE_TIME = 60_000;

    private final ServerController serverController;
    private final PlayerController playerController;
    private final MapManager mapManager;
    private final ClientManager clientManager;

    final Map<Client, Long> lastUpdateMap = new WeakHashMap<>();

    @Inject
    public MapStatListener(ServerController serverController, PlayerController playerController, MapManager mapManager, ClientManager clientManager) {
        this.serverController = serverController;
        this.playerController = playerController;
        this.mapManager = mapManager;
        this.clientManager = clientManager;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addExitHandler(GameState.STARTING, oldState -> {
            playerController.getParticipants().values()
                    .forEach(this::updateParticipantTime);
        });
        serverController.getStateMachine().addExitHandler(GameState.ENDING, oldState -> {
            playerController.getParticipants().values()
                    .forEach(this::updateParticipantTime);
        });
    }

    @UpdateEvent(delay = UPDATE_TIME)
    public void onUpdate() {
        playerController.getParticipants().values()
                        .forEach(this::updateParticipantTime);
    }

    @EventHandler
    public void onLogin(ClientJoinEvent event) {
        lastUpdateMap.put(event.getClient(), System.currentTimeMillis());
    }


    @EventHandler
    public void onLogout(ClientQuitEvent event) {
        final long lastUpdate = lastUpdateMap.remove(event.getClient());
        final long currentTime = System.currentTimeMillis();
        event.getClient().getStatContainer().incrementStat(ClientStat.TIME_PLAYED, currentTime - lastUpdate);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectate(ParticipantStartSpectatingEvent event) {
        updateParticipantTIme(event.getParticipant(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStopSpectate(ParticipantStopSpectatingEvent event) {
        lastUpdateMap.put(event.getParticipant().getClient(), System.currentTimeMillis());
    }

    private void updateParticipantTime(Participant participant) {
        updateParticipantTIme(participant, false);
    }

    private void updateParticipantTIme(Participant participant, boolean force) {
        final Client client = participant.getClient();
        final long currentTime = System.currentTimeMillis();
        final long lastUpdate = lastUpdateMap.computeIfAbsent(client, k -> currentTime);
        final String gameName = serverController.getCurrentState().isInLobby() ? "Lobby" : serverController.getCurrentGame().getConfiguration().getName();
        final String mapName = serverController.getCurrentState().isInLobby() ? mapManager.getWaitingLobby().getMetadata().getName() : mapManager.getCurrentMap().getMetadata().getName();
        if (!participant.isSpectating() || force) {
            final MapPlayedTimeStat mapStat = MapPlayedTimeStat.builder()
                    .gameName(gameName)
                    .mapName(mapName)
                    .build();
            client.getStatContainer().incrementStat(mapStat, currentTime - lastUpdate);
        }
        lastUpdateMap.put(client, currentTime);
    }
}
