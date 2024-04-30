package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.ClanLogger;
import me.mykindos.betterpvp.clans.logging.types.formatted.KillClanLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@Singleton
@SubCommand(ClanCommand.class)
public class KillLogsSubCommand extends ClanSubCommand {
    private final ClanLogger clanLogger;
    @Inject
    public KillLogsSubCommand(ClanManager clanManager, ClientManager clientManager, ClanLogger clanLogger) {
        super(clanManager, clientManager);
        this.clanLogger = clanLogger;
    }

    @Override
    public String getName() {
        return "killlogs";
    }

    @Override
    public String getDescription() {
        return "Get kill logs associated with your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        int numPerPage = 10;
        int pageNumber = 1;

        if (args.length >= 1) {
            try {
                pageNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                //pass
            }
        }

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, "Clans", "You must be in a Clan to run this command");
            return;
        }
        int finalPageNumber = pageNumber;
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            List<KillClanLog> logs = clanLogger.getClanKillLogs(clan);
            int count = 0;
            int start = (finalPageNumber - 1) * numPerPage;
            int end = start + numPerPage;
            int size = logs.size();
            int totalPages = size /numPerPage;
            if (size % numPerPage > 0) {
                totalPages++;
            }
            UtilMessage.message(player, "Clans",
                    UtilMessage.deserialize("<dark_aqua>" + clan.getName() + "</dark_aqua>'s clan kill logs: <white>"
                            + finalPageNumber + "<gray> / <white>" + totalPages));
            if (start <= size) {
                if (end > size) end = size;
                for (KillClanLog log : logs.subList(start, end)) {
                    if (count == numPerPage) break;
                    UtilMessage.message(player, "Clan", log.getComponent());
                    count++;
                }
            }
        });
    }
}