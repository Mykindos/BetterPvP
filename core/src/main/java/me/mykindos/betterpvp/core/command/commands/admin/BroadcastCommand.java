package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class BroadcastCommand extends Command {
    private final ClientManager clientManager;

    @Inject
    public BroadcastCommand(ClientManager clientManager){
        this.clientManager = clientManager;

        aliases.add("bc");
    }

    @Override
    public String getName() {
        return "broadcast";
    }

    @Override
    public String getDescription() {
        return "Send an emphasized message to the server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }
        Component message = Component.empty().append(client.getRank().getPlayerNameMouseOver(player.getName()).decorate(TextDecoration.BOLD))
                        .append(UtilMessage.deserialize(" <red>%s", String.join(" ", args)));
        for (Player playerToSend : Bukkit.getOnlinePlayers()) {
            UtilMessage.message(playerToSend, message);
            UtilSound.playSound(playerToSend, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f, true);
        }
    }
}
