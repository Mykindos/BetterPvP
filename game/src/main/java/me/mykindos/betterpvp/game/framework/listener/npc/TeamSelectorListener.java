package me.mykindos.betterpvp.game.framework.listener.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.framework.manager.TeamSelectorManager;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import me.mykindos.betterpvp.game.framework.model.team.TeamSelector;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import me.mykindos.betterpvp.game.framework.state.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/**
 * Handles player interactions with team selectors
 */
@BPvPListener
@Singleton
@CustomLog
public class TeamSelectorListener implements Listener {

    private final PlayerController playerController;
    private final ServerController serverController;
    private final TeamSelectorManager teamSelectorManager;
    private final MappedWorld waitingLobby;

    @Inject
    public TeamSelectorListener(PlayerController playerController, ServerController serverController,
                                TeamSelectorManager teamSelectorManager, @Named("Waiting Lobby") MappedWorld waitingLobby) {
        this.playerController = playerController;
        this.serverController = serverController;
        this.teamSelectorManager = teamSelectorManager;
        this.waitingLobby = waitingLobby;
        setupStateHandlers();
    }

    private void setupStateHandlers() {
        // Add team selectors when entering WAITING state
        serverController.getStateMachine().addEnterHandler(GameState.WAITING, oldState -> createTeamSelectors());

        // Clean up team selectors when exiting WAITING state
        serverController.getStateMachine().addExitHandler(GameState.STARTING, newState -> teamSelectorManager.clearSelectors());
    }

    private void createTeamSelectors() {
        if (serverController.getCurrentGame() instanceof TeamGame) {
            teamSelectorManager.createTeamSelectors(waitingLobby);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameChange(GameChangeEvent event) {
        if (serverController.getCurrentState() == GameState.WAITING || serverController.getCurrentState() == GameState.STARTING) {
            createTeamSelectors();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (serverController.getCurrentState() != GameState.WAITING && serverController.getCurrentState() != GameState.STARTING) {
            return;
        }

        // Check if current game is a team game
        if (!(serverController.getCurrentGame() instanceof TeamGame<?> teamGame)) {
            return;
        }

        final Player player = event.getPlayer();
        final Participant participant = playerController.getParticipant(player);
        if (participant.isSpectating()) {
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

        // Add player to team
        boolean success = teamGame.addPlayerToTeam(participant, team);

        if (success) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            log.info("Player {} joined team {}", player.getName(), properties.name()).submit();
            UtilMessage.message(player, "Team", Component.text("You joined ", NamedTextColor.GRAY)
                    .append(Component.text(properties.name(), properties.color(), TextDecoration.BOLD))
                    .append(Component.text(" team.", NamedTextColor.GRAY)));
        } else {
            UtilMessage.simpleMessage(player, "<red>This team is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 2.0f);
        }
    }
}