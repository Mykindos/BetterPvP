package me.mykindos.betterpvp.core.chat.commands;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

public class ChannelCommand extends Command {
    @Override
    public String getName() {
        return "channel";
    }

    @Override
    public String getDescription() {
        return "Change chat channel";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

    }
}
