package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.chat.channels.StaffChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class StaffChatCommand extends Command {

    private final Core core;
    private final ClientManager clientManager;
    private final IChatChannel staffChatChannel;

    @Inject
    public StaffChatCommand(Core core, ClientManager clientManager){
        this.core = core;
        this.clientManager = clientManager;
        this.staffChatChannel = new StaffChatChannel(clientManager);

        aliases.add("sc");
    }

    @Override
    public String getName() {
        return "staffchat";
    }

    @Override
    public String getDescription() {
        return "Toggle staff only chat";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        if (args.length > 0) {
            UtilServer.callEventAsync(core, new ChatSentEvent(player, staffChatChannel, Component.text(UtilFormat.spoofNameForLunar(player.getName()) + ": "),
                    Component.text(String.join(" ", args))));
            return;
        }

        if (gamer.getChatChannel().equals(staffChatChannel)) {
            gamer.setChatChannel(ChatChannel.SERVER);
        } else {
            gamer.setChatChannel(ChatChannel.STAFF);
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.TRIAL_MOD;
    }


}
