package me.mykindos.betterpvp.game.framework.listener.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.events.PlayerChangeChatChannelEvent;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.event.GameChangeEvent;
import me.mykindos.betterpvp.game.framework.model.player.Participant;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStartSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.player.event.ParticipantStopSpectatingEvent;
import me.mykindos.betterpvp.game.framework.model.team.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
    private final ClientManager clientManager;

    @Inject
    public PlayerChatListener(PlayerController playerController, ServerController serverController, ClientManager clientManager) {
        this.playerController = playerController;
        this.serverController = serverController;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onChat(ChatSentEvent event) {
        final Player player = event.getPlayer();
        ChatChannel chatChannel = event.getChannel().getChannel();
        if (!(chatChannel == ChatChannel.SERVER || chatChannel == ChatChannel.TEAM || chatChannel == ChatChannel.SPECTATOR)) {
            return; // Only public chat
        }

        final Participant participant = playerController.getParticipant(player);
        final String playerName = UtilFormat.spoofNameForLunar(event.getPlayer().getName());

        if (event.getMessage() instanceof TextComponent textComponent &&
                serverController.getCurrentGame() instanceof TeamGame<?> teamGame &&
                chatChannel == ChatChannel.SERVER
        ) {
            final Team playerTeam = teamGame.getPlayerTeam(player);
            if (textComponent.content().startsWith("#")) {
                TextComponent newText = textComponent.content(textComponent.content().substring(1));

                if (playerTeam != null) {
                    chatChannel = ChatChannel.TEAM;
                    event.setChannel(playerTeam.getTeamChatChannel());
                    event.setMessage(newText);
                } else if (participant.isSpectating()) {
                    chatChannel = ChatChannel.SPECTATOR;
                    event.setChannel(playerController.getSpectatorChatChannel());
                    event.setMessage(newText);
                }
            }
        }

        Component teamPrefix = Component.empty();
        if (chatChannel == ChatChannel.TEAM) {
            teamPrefix = Component.text("Team ", NamedTextColor.WHITE);
        } else if (chatChannel == ChatChannel.SPECTATOR) {
            teamPrefix = Component.text("Spec ", NamedTextColor.WHITE);
        }

        // Spectators
        if (participant.isSpectating()) {
            event.setPrefix(teamPrefix.append(Component.text(player.getName(), NamedTextColor.GRAY)).appendSpace());
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
            event.setPrefix(teamPrefix.append(Component.text("Dead", NamedTextColor.GRAY))
                    .appendSpace()
                    .append(Component.text(playerName, playerColor))
                    .appendSpace());
        }
        // Alive
        else {
            event.setPrefix(teamPrefix.append(Component.text(playerName, playerColor)).appendSpace());
        }
    }

    @EventHandler
    public void onPlayerStartSpectating(ParticipantStartSpectatingEvent event) {
        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        if (gamer.getChatChannel().getChannel() != ChatChannel.TEAM) return;
        gamer.setChatChannel(ChatChannel.SERVER);
    }

    @EventHandler
    public void onPlayerStopSpectating(ParticipantStopSpectatingEvent event) {
        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        if (gamer.getChatChannel().getChannel() != ChatChannel.SPECTATOR) return;
        gamer.setChatChannel(ChatChannel.SERVER);
    }

    @EventHandler
    public void onPlayerChangeChatChannel(final PlayerChangeChatChannelEvent event) {
        final Player player = event.getGamer().getPlayer();
        if (event.getTargetChannel() == ChatChannel.TEAM) {
            if (serverController.getCurrentGame() instanceof TeamGame<?> teamGame) {
                final Team playerTeam = teamGame.getPlayerTeam(player);
                if (playerTeam == null) {
                    event.cancel("Not on a team");
                    return;
                }
                event.setNewChannel(playerTeam.getTeamChatChannel());
            }
        } else if (event.getTargetChannel() == ChatChannel.SPECTATOR) {
            event.setNewChannel(playerController.getSpectatorChatChannel());
        }
    }

    @EventHandler
    public void onGameChange(final GameChangeEvent event) {
        if (event.getNewGame() instanceof TeamGame<?>) return;
        playerController.getEverybody().keySet().stream()
                .map(player -> clientManager.search().online(player).getGamer())
                .filter(gamer -> gamer.getChatChannel().getChannel() == ChatChannel.TEAM)
                .forEach(gamer -> gamer.setChatChannel(ChatChannel.SERVER));
    }


}
