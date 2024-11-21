package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class SetAdminCommand extends ClanSubCommand {

    @Inject
    public SetAdminCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "setadmin";
    }

    @Override
    public String getDescription() {
        return "Set whether a clan is an admin clan or not";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 1) {
            Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
            boolean isAdmin = Boolean.parseBoolean(args[0]);
            clan.setSafe(isAdmin);
            UtilMessage.message(player, "Clans", "Updated clan admin status to " + isAdmin);
            clanManager.getRepository().updateClanAdmin(clan);
            clientManager.sendMessageToRank("Clans", UtilMessage.deserialize( "<yellow>%s<gray> set <yellow>%s<gray> as an admin clan: <green>%s",
                    player.getName(), clan.getName(), isAdmin), Rank.ADMIN);
        } else {
            UtilMessage.message(player, "Clans", "Correct usage: /clan setadmin <true/false>");
        }

    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ArgumentType.BOOLEAN.name() : ArgumentType.NONE.name();
    }

}
