package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.menu.PlayersOfClanMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class GetPlayersOfClanSubCommand extends ClanSubCommand {


    @Inject
    public GetPlayersOfClanSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "clanplayers";
    }

    @Override
    public String getDescription() {
        return "Get players associated with a clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length < 1) {
            UtilMessage.message(player, "Clan", getUsage());
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByName(args[0]);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clan", "<green>%s</green> is not a valid clanname", args[0]);
            return;
        }
        Clan clan = clanOptional.get();

        new PlayersOfClanMenu(clan.getName(), clan.getId(), clanManager, clientManager, null).show(player);
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ClanArgumentType.CLAN.name();
        }
        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
