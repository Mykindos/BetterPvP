package me.mykindos.betterpvp.game.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Singleton
public class SpectateCommand extends Command {

    private final PlayerController playerController;
    private final ServerController serverController;

    @Inject
    public SpectateCommand(PlayerController playerController, ServerController serverController) {
        this.playerController = playerController;
        this.serverController = serverController;
        aliases.add("spec");
    }

    @Override
    public String getName() {
        return "spectate";
    }

    @Override
    public String getDescription() {
        return "Spectate the next game";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING) {
            UtilMessage.simpleMessage(player, "Game", "You cannot toggle spectator while a game is in progress.");
            return;
        }

        final Participant participant = playerController.getParticipant(player);
        final boolean wasSpectating = participant.isSpectateNextGame();
        playerController.setSpectating(player, participant, !wasSpectating, true);

        if (participant.isSpectateNextGame()) {
            UtilMessage.simpleMessage(player, "Game", "You will now spectate the next game.");
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f).play(player);
        } else {
            UtilMessage.simpleMessage(player, "Game", "You will no longer spectate the next game.");
            new SoundEffect(Sound.UI_BUTTON_CLICK, 0.75f, 1.0f).play(player);
        }
    }
}
