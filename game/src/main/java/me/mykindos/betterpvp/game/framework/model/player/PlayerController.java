package me.mykindos.betterpvp.game.framework.model.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.chat.SpectatorChatChannel;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantDeathEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantReviveEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Controls all players and their states within the game.
 */
@Singleton
public class PlayerController {

    private final Map<Player, Participant> players = new WeakHashMap<>();
    private final GamePlugin plugin;
    private final ServerController serverController;
    @Getter
    private final IChatChannel spectatorChatChannel = new SpectatorChatChannel(this);

    @Inject
    public PlayerController(GamePlugin plugin, ServerController serverController) {
        this.plugin = plugin;
        this.serverController = serverController;
    }

    public Map<Player, Participant> getParticipants() {
        return players.entrySet().stream()
                .filter(entry -> !entry.getValue().isSpectating() && entry.getKey().isOnline())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Player, Participant> getSpectators() {
        return players.entrySet().stream()
                .filter(entry -> entry.getValue().isSpectating())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a Map of spectators only spectating this game
     * @return
     */
    public Map<Player, Participant> getThisGameSpectators() {
        return players.entrySet().stream()
                .filter(entry -> !entry.getValue().isSpectateNextGame() && entry.getValue().isSpectating())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Player, Participant> getEverybody() {
        return Collections.unmodifiableMap(players);
    }

    public @NotNull Participant getParticipant(Player player) {
        return players.get(player);
    }

    public void registerPlayer(Player player, Participant participant) {
        players.put(player, participant);
        updatePlayerCapabilities(player, participant);
    }

    public void setAlive(Player player, Participant participant, boolean alive) {
        boolean old = participant.alive;
        participant.alive = alive;

        if (old != alive) {
            if (alive) {
                new ParticipantReviveEvent(player, participant).callEvent();
            } else {
                new ParticipantDeathEvent(player, participant).callEvent();
            }
        }

        updatePlayerCapabilities(player, participant);
    }

    public void setSpectating(Player player, Participant participant, boolean spectating, boolean persist) {
        boolean old = participant.spectating;
        boolean oldPersist = participant.spectateNextGame;
        participant.spectating = spectating;
        participant.spectateNextGame = spectating && persist;

        if (old != spectating || oldPersist != persist) {
            if (spectating) {
                new ParticipantStartSpectatingEvent(player, participant).callEvent();
            } else {
                new ParticipantStopSpectatingEvent(player, participant).callEvent();
            }
        }

        updatePlayerCapabilities(player, participant);
    }

    public void updatePlayerCapabilities(Player player, Participant participant) {
        UtilServer.runTaskLater(plugin, () -> {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setSpectatorTarget(null);
            }

            final GameState state = serverController.getCurrentState();
            switch (state) {
                case WAITING, STARTING -> {
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setInvulnerable(true);
                }
                case IN_GAME, ENDING -> {
                    if (participant.isSpectating()) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setInvulnerable(true);
                        return;
                    }

                    if (!participant.isAlive()) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setInvulnerable(true);
                        return;
                    }

                    // alive
                    player.setGameMode(GameMode.ADVENTURE);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setInvulnerable(false);
                }
            }

            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
        }, 1L);
    }

    public void unregisterPlayer(Player player) {
        players.remove(player);
    }
}
