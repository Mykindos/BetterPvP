package me.mykindos.betterpvp.game.framework.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.framework.model.team.TeamSelector;
import me.mykindos.betterpvp.game.framework.manager.TeamSelectorManager;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Optional;

/**
 * Handles player interactions with team selectors
 */
@BPvPListener
@Singleton
@CustomLog
public class TeamSelectorListener implements Listener {

    private final ServerController serverController;
    private final TeamSelectorManager teamSelectorManager;
    private final MappedWorld waitingLobby;

    @Inject
    public TeamSelectorListener(ServerController serverController, TeamSelectorManager teamSelectorManager,
                                @Named("Waiting Lobby") MappedWorld waitingLobby) {
        this.serverController = serverController;
        this.teamSelectorManager = teamSelectorManager;
        this.waitingLobby = waitingLobby;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // Add team selectors when entering WAITING state
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> {
            if (serverController.getCurrentGame() instanceof TeamGame) {
                teamSelectorManager.createTeamSelectors(waitingLobby);
            }
        });

        // Clean up team selectors when exiting WAITING state
        serverController.getStateMachine().addExitHandler(GameState.WAITING, newState -> {
            teamSelectorManager.clearSelectors();
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (serverController.getCurrentState() != GameState.IN_GAME) {
            return;
        }

        // Check if current game is a team game
        if (!(serverController.getCurrentGame() instanceof TeamGame teamGame)) {
            return;
        }

        final Optional<TeamSelector> selectorOpt = teamSelectorManager.getTeamSelectors().stream()
                .filter(selector -> selector.getEntity().equals(event.getRightClicked()))
                .findFirst();
        if (selectorOpt.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        // Get team from selector
        final TeamSelector selector = selectorOpt.get();
        final TeamProperties properties = selector.getTeamProperties();
        final Team team = teamGame.getTeam(properties);
        final Player player = event.getPlayer();

        // Add player to team
        boolean success = teamGame.addPlayerToTeam(player, team);

        if (success) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            log.info("Player {} joined team {}", player.getName(), properties.name()).submit();
        } else {
            UtilMessage.simpleMessage(player, "<red>This team is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}