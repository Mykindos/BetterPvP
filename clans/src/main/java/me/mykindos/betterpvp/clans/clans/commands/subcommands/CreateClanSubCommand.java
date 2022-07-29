package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.ISubCommand;
import org.bukkit.entity.Player;

public class CreateClanSubCommand implements ISubCommand {

    @Inject
    private ClanManager clanManager;

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {

    }


}
