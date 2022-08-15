package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.*;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.List;

public class ClanCommand extends Command {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @WithReflection
    @Inject
    public ClanCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;

        aliases.addAll(List.of("c", "f", "faction"));

        subCommands.add(new InfoSubCommand(clanManager, gamerManager));
        subCommands.add(new CreateClanSubCommand(clanManager, gamerManager));
        subCommands.add(new DisbandClanSubCommand(clanManager, gamerManager));
        subCommands.add(new ClaimSubCommand(clanManager, gamerManager));
        subCommands.add(new InviteSubCommand(clanManager, gamerManager));
        subCommands.add(new JoinSubCommand(clanManager, gamerManager));
        subCommands.add(new LeaveSubCommand(clanManager, gamerManager));
        subCommands.add(new KickSubCommand(clanManager, gamerManager));
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
        UtilMessage.simpleMessage(player, "Test", "<rainbow>%s <yellow>%s <green>%s", "Testing", "123", "456");
    }
}
