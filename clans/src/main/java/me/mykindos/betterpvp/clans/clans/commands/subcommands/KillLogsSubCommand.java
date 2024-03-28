package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.ClanLogger;
import me.mykindos.betterpvp.clans.logging.types.formatted.FormattedClanLog;
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
        int amount = 5;

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, "Clans", "You must be in a Clan to run this command");
            return;
        }

        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                UtilMessage.message(player, "Clans", UtilMessage.deserialize("<green>%s</green> is not a valid integer. Integer must be >= 1.", args[1]));
                return;
            }
        }

        final int finalAmount = amount;
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            List<FormattedClanLog> logs = clanLogger.getClanKillLogs(clan.getId(), finalAmount);
            UtilMessage.message(player, "Clan", "Retrieving the last <green>%s</green> logs", finalAmount);

            for (FormattedClanLog log : logs) {
                UtilMessage.message(player, "Clan", log.getComponent());
            }
        });
    }
}