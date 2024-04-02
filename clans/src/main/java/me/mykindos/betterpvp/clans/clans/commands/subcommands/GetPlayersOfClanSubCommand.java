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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class GetPlayersOfClanSubCommand extends ClanSubCommand {

    private final ClanLogger clanLogger;

    @Inject
    public GetPlayersOfClanSubCommand(ClanManager clanManager, ClientManager clientManager, ClanLogger clanLogger) {
        super(clanManager, clientManager);
        this.clanLogger = clanLogger;
    }

    @Override
    public String getName() {
        return "clanmembers";
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


        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
            List<UUID> clanMembers = clan.getMembers().stream().map(clanMember -> UUID.fromString(clanMember.getUuid())).toList();
            List<OfflinePlayer> offlinePlayers = clanLogger.getPlayersByClan(clan.getId());
            UtilMessage.message(player, "Clan", "Retrieving past and present Clan members of <aqua>%s</aqua>", clan.getName());

            for (OfflinePlayer offlinePlayer : offlinePlayers) {

                if (clanMembers.contains(offlinePlayer.getUniqueId())) {
                    UtilMessage.message(player, "Clan", UtilMessage.deserialize(UtilFormat.getOnlineStatus(offlinePlayer.getUniqueId()) + offlinePlayer.getName()));
                    continue;
                }
                UtilMessage.message(player, "Clan", "<white>%s</white>", offlinePlayer.getName());
            }
        });
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
