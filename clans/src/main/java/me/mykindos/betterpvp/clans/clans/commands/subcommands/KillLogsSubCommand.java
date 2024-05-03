package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.KillClanLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

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
            List<KillClanLog> logs = clanManager.getRepository().getClanKillLogs(clan);
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
                    sendMessage(player, clan, log);
                    count++;
                }
            }
        });
    }

    private void sendMessage(Player player, Clan clan, KillClanLog killLog) {
        clientManager.search().offline(killLog.getKiller(), killerClientOpt -> {
            clientManager.search().offline(killLog.getVictim(), victimClientOpt -> {
                if (killerClientOpt.isPresent() && victimClientOpt.isPresent()) {

                    Optional<Clan> killerClan = clanManager.getClanById(killLog.getKillerClan());
                    if(killerClan.isEmpty()) return;

                    Optional<Clan> victimClan = clanManager.getClanById(killLog.getVictimClan());
                    if(victimClan.isEmpty()) return;

                    Client killerClient = killerClientOpt.get();
                    Client victimClient = victimClientOpt.get();

                    ClanRelation killerRelation = clan.getRelation(killerClan.get());
                    ClanRelation victimRelation = clan.getRelation(victimClan.get());

                    Component component = killLog.getTimeComponent().append(Component.text("- ", NamedTextColor.GRAY))
                            .append(Component.text(killerClient.getName(), killerRelation.getPrimary()))
                            .append(Component.text(" killed ", NamedTextColor.GRAY))
                            .append(Component.text(victimClient.getName(), victimRelation.getPrimary()))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(killLog.getDominance(), NamedTextColor.YELLOW))
                            .append(Component.text(")"));

                    UtilMessage.message(player, "Clans", component);
                }
            });
        });

    }
}