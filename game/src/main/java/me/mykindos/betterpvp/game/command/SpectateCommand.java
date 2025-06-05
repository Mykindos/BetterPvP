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
        final boolean inGame = serverController.getCurrentState() == GameState.IN_GAME || serverController.getCurrentState() == GameState.ENDING;
        if (inGame && !serverController.getCurrentGame().getConfiguration().getAllowLateJoinsAttribute().getValue()) {
            UtilMessage.simpleMessage(player, "Game", "You cannot toggle spectator while a game is in progress.");
            return;
        }

        if (playerController.getParticipant(player).isAlive() && client.getGamer().isInCombat()) {
            UtilMessage.simpleMessage(player, "Game", "You may not toggle spectator while in combat");
            return;
        }

        final Participant participant = playerController.getParticipant(player);
        boolean toSpectate = !participant.isSpectateNextGame();
        boolean shouldPersist = true;
        if (inGame) {
            //set player to spectate if in game, and let spectate logic handle it
            toSpectate = true;
            //toggle persist
            if (participant.isSpectating()) {
                shouldPersist = !participant.isSpectateNextGame();
            }
        }
        playerController.setSpectating(player, participant, toSpectate, shouldPersist);

        if (participant.isSpectateNextGame()) {
            UtilMessage.simpleMessage(player, "Game", "You will now spectate the next game.");
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1.0f, 1.0f).play(player);
        } else if (!inGame || !participant.isAlive()) {
            UtilMessage.simpleMessage(player, "Game", "You will no longer spectate the next game.");
            new SoundEffect(Sound.UI_BUTTON_CLICK, 0.75f, 1.0f).play(player);
        }
    }
}
