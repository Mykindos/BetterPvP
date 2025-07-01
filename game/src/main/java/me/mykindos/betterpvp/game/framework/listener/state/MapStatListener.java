package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.game.MapPlayedTimeStat;
import me.mykindos.betterpvp.core.client.stats.listeners.TimedStatListener;
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

@Singleton
@BPvPListener
public class MapStatListener extends TimedStatListener {

    private final ServerController serverController;
    private final PlayerController playerController;
    private final MapManager mapManager;

    @Inject
    public MapStatListener(ServerController serverController, PlayerController playerController, MapManager mapManager, ClientManager clientManager) {
        super(clientManager);
        this.serverController = serverController;
        this.playerController = playerController;
        this.mapManager = mapManager;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectate(ParticipantStartSpectatingEvent event) {
        //because spectators are now spectating as of this event, we need to do some custom logic to save their time played
        final long currentTime = System.currentTimeMillis();
        final long lastUpdate = lastUpdateMap.computeIfAbsent(event.getPlayer().getUniqueId(), k -> currentTime);
        updateParticipantTime(event.getParticipant(), currentTime - lastUpdate, true);
        lastUpdateMap.put(event.getPlayer().getUniqueId(), currentTime);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStopSpectate(ParticipantStopSpectatingEvent event) {
        lastUpdateMap.put(event.getParticipant().getPlayer().getUniqueId(), System.currentTimeMillis());
    }


    @Override
    public void onUpdate(Client client, long deltaTime) {
        final Participant participant = playerController.getParticipant(client.getGamer().getPlayer());
        updateParticipantTime(participant, deltaTime, false);
    }

    private void updateParticipantTime(Participant participant) {
        final long currentTime = System.currentTimeMillis();
        final long lastUpdate = lastUpdateMap.computeIfAbsent(participant.getPlayer().getUniqueId(), k -> currentTime);
        updateParticipantTime(participant, currentTime - lastUpdate, false);
    }

    private void updateParticipantTime(Participant participant, long deltaTime, boolean force) {
        final Client client = participant.getClient();
        final String gameName = serverController.getCurrentState().isInLobby() ? "Lobby" : serverController.getCurrentGame().getConfiguration().getName();
        final String mapName = serverController.getCurrentState().isInLobby() ? mapManager.getWaitingLobby().getMetadata().getName() : mapManager.getCurrentMap().getMetadata().getName();
        if (!participant.isSpectating() || force) {
            final MapPlayedTimeStat mapStat = MapPlayedTimeStat.builder()
                    .gameName(gameName)
                    .mapName(mapName)
                    .build();
            client.getStatContainer().incrementStat(mapStat, deltaTime);
        } //todo else, increment spectating time
    }

}
