package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import org.bukkit.entity.Player;


public class CreateClanSubCommand extends SubCommand {

    @Inject
    private ClanManager clanManager;

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a clan";
    }

    @Override
    public void execute(Player player, Client client, String[] args) {

    }




}
