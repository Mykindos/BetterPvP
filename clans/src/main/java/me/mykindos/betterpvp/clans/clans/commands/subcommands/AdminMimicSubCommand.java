package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AdminMimicSubCommand extends ClanSubCommand {

    @Inject
    public AdminMimicSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "x";
    }

    @Override
    public String getDescription() {
        return "Set a Clan to mimic as.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan to mimic");
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name");
            return;
        }

        Clan targetClan = targetClanOptional.get();


    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
