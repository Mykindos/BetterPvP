package me.mykindos.betterpvp.clans.clans.commands;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

public class ClanCommand extends Command {
    @Override
    public String getName() {
        return "clan";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"c"};
    }

    @Override
    public String getDescription() {
        return "Basic clan command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        player.sendMessage("Hi");
    }
}
