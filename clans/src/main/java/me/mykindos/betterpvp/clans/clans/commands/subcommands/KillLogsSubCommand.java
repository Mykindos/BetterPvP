package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.menu.ClanKillLogMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class KillLogsSubCommand extends ClanSubCommand {

    @Inject
    public KillLogsSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "killlogs";
    }

    @Override
    public String getDescription() {
        return "clans.command.kill-logs.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kill-logs.no-clan");
            return;
        }
        new ClanKillLogMenu(clan, clanManager).show(player);
    }

}