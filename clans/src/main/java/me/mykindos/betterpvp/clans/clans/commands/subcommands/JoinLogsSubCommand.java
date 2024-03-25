package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.ClanLogger;
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
public class JoinLogsSubCommand extends ClanSubCommand {

    @Inject
    public JoinLogsSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "joinlogs";
    }

    @Override
    public String getDescription() {
        return "Get logs associated with joining/leaving clan";
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
            List<String> logs = ClanLogger.getJoinLeaveMessages(clan.getId(), finalAmount);
            UtilMessage.message(player, "Clan", "Retrieving the last <green>%s</green> logs", finalAmount);

            for (String log : logs) {
                UtilMessage.message(player, "Clan", UtilMessage.deserialize("<white>" + log + "</white>"));
            }
        });
    }
}