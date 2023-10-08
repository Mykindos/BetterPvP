package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@Singleton
@SubCommand(ClanCommand.class)
public class SetSafeCommand extends ClanSubCommand {

    @Inject
    public SetSafeCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "setsafe";
    }

    @Override
    public String getDescription() {
        return "Set whether a clan's territory is a safezone or not";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 1) {
            Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
            boolean isSafe = Boolean.parseBoolean(args[0]);
            clan.setSafe(isSafe);
            UtilMessage.message(player, "Clans", "Updated clan safe status to " + isSafe);
            clanManager.getRepository().updateClanSafe(clan);
        } else {
            UtilMessage.message(player, "Clans", "Correct usage: /clan setsafe <true/false>");
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
