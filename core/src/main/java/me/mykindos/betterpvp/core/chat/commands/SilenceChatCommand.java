package me.mykindos.betterpvp.core.chat.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class SilenceChatCommand extends Command implements Listener  {

    private final ClientManager clientManager;
    private boolean isChatSilenced = false;

    @Inject
    public SilenceChatCommand(ClientManager clientManager){
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "silencechat";
    }

    @Override
    public String getDescription() {
        return "Prevent non-ranked players from sending messages";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (isChatSilenced) {
            UtilMessage.broadcast("Chat", "Chat is no longer silenced");
            isChatSilenced = false;
            return;
        }
        UtilMessage.broadcast("Chat", "Chat is now silenced");
        isChatSilenced = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatSent(ChatSentEvent event) {
        if (isChatSilenced && !clientManager.search().online(event.getPlayer()).hasRank(Rank.HELPER)) {
            UtilMessage.message(event.getPlayer(), "Chat", "Chat is currently silenced, you cannot send a message in global chat");
            event.cancel("Chat is silenced");
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.NONE.name();
    }


}
