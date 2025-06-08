package me.mykindos.betterpvp.game.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class SpectatorChatCommand extends Command {
    private final PlayerController playerController;

    @Inject
    public SpectatorChatCommand(PlayerController playerController) {
        this.playerController = playerController;
        aliases.add("specchat");
    }

    @Override
    public String getName() {
        return "spectatorchat";
    }

    @Override
    public String getDescription() {
        return "Toggle spectator only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!(playerController.getParticipant(player).isSpectating())) {
            UtilMessage.message(player, "Game", "You must be spectating to use spectator chat");
            return;
        }
        final Gamer gamer = client.getGamer();
        if (args.length > 0) {
            UtilServer.callEventAsync(JavaPlugin.getPlugin(GamePlugin.class), new ChatSentEvent(player, playerController.getSpectatorChatChannel(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "),
                    Component.text(String.join(" ", args))));
            return;
        }

        if (gamer.getChatChannel().equals(playerController.getSpectatorChatChannel())) {
            gamer.setChatChannel(ChatChannel.SERVER);
        } else {
            gamer.setChatChannel(ChatChannel.SPECTATOR);
        }
    }
}
