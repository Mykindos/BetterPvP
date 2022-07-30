package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.CreateClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.entity.Player;

import java.util.List;

public class ClanCommand extends Command {

    private final ClanManager clanManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager) {
        this.clanManager = clanManager;

        aliases.addAll(List.of("c", "f", "faction"));

        subCommands.add(new CreateClanSubCommand());
    }

    @Override
    public String getName() {
        return "clan";
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
