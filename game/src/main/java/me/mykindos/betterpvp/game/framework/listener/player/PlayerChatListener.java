package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class PlayerChatListener implements Listener {

    private final PlayerController playerController;
    private final ServerController serverController;

    @Inject
    public PlayerChatListener(PlayerController playerController, ServerController serverController) {
        this.playerController = playerController;
        this.serverController = serverController;
    }

    @EventHandler
    public void onChat(ChatSentEvent event) {
        final Player player = event.getPlayer();
        if (event.getChannel().getChannel() != ChatChannel.SERVER) {
            return; // Only public chat
        }

        final Participant participant = playerController.getParticipant(player);
        String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        // Spectators
        if (participant.isSpectating()) {
            event.setPrefix(Component.text(player.getName(), NamedTextColor.GRAY).appendSpace());
            return;
        }

        TextColor playerColor;
        if (serverController.getCurrentGame() != null) {
            playerColor = serverController.getCurrentGame().getConfiguration().getPlayerColorProvider().getColor(player, serverController.getCurrentGame());
        } else {
            playerColor = NamedTextColor.YELLOW;
        }

        // Dead
        if (!participant.isAlive()) {
            event.setPrefix(Component.text("Dead", NamedTextColor.GRAY)
                    .appendSpace()
                    .append(Component.text(playerName, playerColor))
                    .appendSpace());
        }
        // Alive
        else {
            event.setPrefix(Component.text(playerName, playerColor).appendSpace());
        }
    }

}
