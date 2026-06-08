package me.mykindos.betterpvp.core.scene.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class NPCCommand extends Command {

    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "core.command.n-p-c.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.help.title");
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.help.spawn");
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.help.remove");
        UtilMessage.message(player, "core.prefix.command", "core.command.npc.help.list");
    }
}
