package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameMapStat;
import me.mykindos.betterpvp.core.client.stats.listeners.TimedStatListener;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@BPvPListener
@CustomLog
public class GameStatListener extends TimedStatListener {
    private final StatManager statManager;
    private final ServerController serverController;
    private final PlayerController playerController;

    private final Map<UUID, Team> playerTeams = new ConcurrentHashMap<>();
    private final Set<UUID> players = new HashSet<>();

    @Inject
    public GameStatListener(ServerController serverController, PlayerController playerController, ClientManager clientManager, StatManager statManager) {
        super(clientManager);
        this.serverController = serverController;
        this.playerController = playerController;
        this.statManager = statManager;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addExitHandler(GameState.STARTING, oldState -> {
            playerController.getParticipants().values()
                    .forEach(this::updateParticipantTime);
        });
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GamePlugin.class), () -> {
                players.addAll(playerController.getParticipants().keySet().stream().map(Player::getUniqueId).collect(Collectors.toSet()));
                if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
                    for (Team team : teamGame.getParticipants()) {
                        for (Player player : team.getPlayers()) {
                            playerTeams.put(player.getUniqueId(), team);
                        }
                    }
                }
            }, 1L);
        });

        serverController.getStateMachine().addEnterHandler(GameState.ENDING, oldState -> {
            onGameEnd();
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
        players.add(event.getPlayer().getUniqueId());
        if (serverController.getCurrentState() == GameState.IN_GAME && serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GamePlugin.class), () -> {
                Team team = teamGame.getPlayerTeam(event.getPlayer());
                if (team == null) {
                    log.warn("Participant {} stopped spectating in a team game but is not on a team", event.getPlayer().getName()).submit();
                    return;
                }
                //always keep original team
                playerTeams.putIfAbsent(event.getPlayer().getUniqueId(), team);
            }, 1L);

        }
    }

    public void onGameEnd() {
        final String gameName = serverController.getCurrentState().isInLobby() ? "Lobby" : serverController.getCurrentGame().getConfiguration().getName();
        final GameMapStat.GameMapStatBuilder<?, ?> builder = GameMapStat.builder()
                .gameName(gameName);
        if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            playerTeams.forEach((id, team) -> {
                statManager.incrementStat(id, builder.action(GameMapStat.Action.MATCHES_PLAYED), 1);
                if (teamGame.getWinners().contains(team)) {
                    statManager.incrementStat(id, builder.action(GameMapStat.Action.WIN), 1);
                } else {
                    statManager.incrementStat(id, builder.action(GameMapStat.Action.LOSS), 1);
                }
            });
        } //todo other games
        playerTeams.clear();
        players.clear();
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
        final GameMapStat.GameMapStatBuilder<?, ?> builder = GameMapStat.builder()
                .gameName(gameName);
        if (!participant.isSpectating() || force) {
            statManager.incrementStat(client.getUniqueId(), builder.action(GameMapStat.Action.TIME_PLAYED), deltaTime);
        } else {
            statManager.incrementStat(client.getUniqueId(), builder.action(GameMapStat.Action.SPECTATE_TIME), deltaTime);
        }
    }

}
