package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import org.bukkit.entity.Player;

public class InfoSubCommand extends ClanSubCommand {
    public InfoSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "View another clans information";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        // TODO implement
    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ClanArgumentType.CLAN.name();
        }

        return ArgumentType.NONE.name();
    }
}
