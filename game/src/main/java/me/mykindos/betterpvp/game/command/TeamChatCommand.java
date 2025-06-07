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
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class TeamChatCommand extends Command {
    private final ServerController serverController;

    @Inject
    public TeamChatCommand(ServerController serverController) {
        this.serverController = serverController;
        aliases.add("tc");
    }

    @Override
    public String getName() {
        return "teamchat";
    }

    @Override
    public String getDescription() {
        return "Toggle team only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!(serverController.getCurrentGame() instanceof TeamGame<?> teamGame)) {
            UtilMessage.message(player, "Game", "You must be in a team game to use team chat");
            return;
        }
        final Gamer gamer = client.getGamer();
        final Team playerTeam = teamGame.getPlayerTeam(player);
        if (playerTeam == null) {
            UtilMessage.message(player, "Clans", "You must be on a Team to send a Team chat");
            return;
        }
        if (args.length > 0) {
            UtilServer.callEventAsync(JavaPlugin.getPlugin(GamePlugin.class), new ChatSentEvent(player, playerTeam.getTeamChatChannel(), Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "),
                    Component.text(String.join(" ", args))));
            return;
        }

        if (gamer.getChatChannel().equals(playerTeam.getTeamChatChannel())) {
            gamer.setChatChannel(ChatChannel.SERVER);
        } else {
            gamer.setChatChannel(ChatChannel.TEAM);
        }
    }
}
