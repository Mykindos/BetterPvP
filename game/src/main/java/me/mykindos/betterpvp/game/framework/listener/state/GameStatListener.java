package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.client.stats.listeners.TimedStatListener;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.manager.MapManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.stats.GameInfo;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Singleton
@BPvPListener
@CustomLog
public class GameStatListener extends TimedStatListener {
    private final StatManager statManager;
    private final MapManager mapManager;
    private final ServerController serverController;
    private final PlayerController playerController;

    @Inject
    public GameStatListener(ServerController serverController, PlayerController playerController, ClientManager clientManager, StatManager statManager, MapManager mapManager) {
        super(clientManager);
        this.serverController = serverController;
        this.playerController = playerController;
        this.statManager = statManager;
        this.mapManager = mapManager;
        //initialize game info to avoid NPE
        serverController.setLobbyGameInfo(new GameInfo(
                GameInfo.LOBBY_GAME_NAME,
                mapManager.getWaitingLobby().getMetadata().getName()
        ));
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        serverController.getStateMachine().addExitHandler(GameState.STARTING, oldState -> {
            playerController.getParticipants().values()
                    .forEach(this::updateParticipantTime);
        });
        serverController.getStateMachine().addEnterHandler(GameState.IN_GAME, oldState -> {
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GamePlugin.class), this::onGameStart, 1L);
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
    public void onPlayerJoin(ClientJoinEvent event) {

        assignGameTeam(event.getClient().getUniqueId());
        //we always need to make sure that there is an entry in game_teams for a player,
        //even if the game eventually crashes
        if (serverController.getCurrentState().isInLobby()) {
            statManager.save(serverController.getLobbyInfo());
            return;
        }
        statManager.save(serverController.getCurrentGame().getGameInfo());
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
        //todo do we update spectate time correctly?
        lastUpdateMap.put(event.getParticipant().getPlayer().getUniqueId(), System.currentTimeMillis());
        if (serverController.getCurrentState() == GameState.IN_GAME && serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GamePlugin.class), () -> {
                Team team = teamGame.getPlayerTeam(event.getPlayer());
                if (team == null) {
                    log.warn("Participant {} stopped spectating in a team game but is not on a team", event.getPlayer().getName()).submit();
                    return;
                }
                //store stats for last team
                teamGame.getGameInfo().getPlayerTeams().put(event.getPlayer().getUniqueId(), team.getProperties().name());
            }, 1L);

        }
    }

    public void assignGameTeam(UUID id) {
        if (serverController.getCurrentState().isInLobby()) {
            serverController.getLobbyInfo().getPlayerTeams().put(id, GameTeamMapStat.NONE_TEAM_NAME);
            return;
        }
        final GameInfo gameInfo = serverController.getCurrentGame().getGameInfo();
        if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            final Team team = teamGame.getPlayerTeam(id);
            gameInfo.getPlayerTeams().put(id, team == null ? GameTeamMapStat.SPECTATOR_TEAM_NAME : team.getProperties().name());
        } else {
            gameInfo.getPlayerTeams().put(id, GameTeamMapStat.NONE_TEAM_NAME);
        }
    }

    public void onGameStart() {
        statManager.save(serverController.getLobbyInfo());
        serverController.setGameGameInfo(new GameInfo(
                serverController.getCurrentGame().getConfiguration().getName(),
                mapManager.getCurrentMap().getName()
        ));
        //initialize teams, to add players that queued for a team before game start
        playerController.getParticipants().keySet().stream()
                .map(Player::getUniqueId)
                .forEach(this::assignGameTeam);
        statManager.save(serverController.getCurrentGameInfo());
    }


    public void onGameEnd() {
        if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
            final GameInfo gameInfo = teamGame.getGameInfo();
            gameInfo.getPlayerTeams().forEach((id, teamName) -> {
                final Team team = teamGame.getTeam(teamName);
                if (teamGame.getWinners().contains(team)) {
                    statManager.incrementGameMapStat(id, GameTeamMapNativeStat.builder().action(GameTeamMapNativeStat.Action.WIN), 1);
                } else if (team != null) {
                    statManager.incrementGameMapStat(id, GameTeamMapNativeStat.builder().action(GameTeamMapNativeStat.Action.LOSS), 1);
                }
            });
        }
        statManager.save(serverController.getCurrentGame().getGameInfo());
        serverController.setLobbyGameInfo(
                new GameInfo(
                        GameInfo.LOBBY_GAME_NAME,
                        mapManager.getWaitingLobby().getMetadata().getName()
                )
        );
        playerController.getParticipants().keySet().stream()
                .map(Player::getUniqueId)
                .forEach(this::assignGameTeam);
        statManager.save(serverController.getCurrentGameInfo());
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

    /**
     *
     * @param participant the participant
     * @param deltaTime the elapsed time (ms)
     * @param force whether to forcefully increment GAME_TIME_PLAYED after a player has just switched to spectator
     */
    private void updateParticipantTime(Participant participant, long deltaTime, boolean force) {
        final Client client = participant.getClient();
        if (!participant.isSpectating() || force) {
            statManager.incrementGameMapStat(client.getUniqueId(), GameTeamMapNativeStat.builder().action(GameTeamMapNativeStat.Action.GAME_TIME_PLAYED), deltaTime);
        } else {
            statManager.incrementGameMapStat(client.getUniqueId(), GameTeamMapNativeStat.builder().action(GameTeamMapNativeStat.Action.SPECTATE_TIME), deltaTime);
        }
    }

}
