package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class AdminLogsSubCommand extends ClanSubCommand {

    private final LogRepository logRepository;

    @Inject
    public AdminLogsSubCommand(ClanManager clanManager, ClientManager clientManager, LogRepository logRepository) {
        super(clanManager, clientManager);
        this.logRepository = logRepository;
    }

    @Override
    public String getName() {
        return "adminlogs";
    }

    @Override
    public String getDescription() {
        return "Get logs associated with the specified clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clanname|clanid>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Logs", getUsage());
        }
        Optional<Clan> clanOptional = clanManager.getClanByName(args[0]);

        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            new CachedLogMenu(clan.getName(), LogContext.CLAN, clan.getId() + "", null, CachedLogMenu.CLANS, JavaPlugin.getPlugin(Clans.class), logRepository, null).show(player);
            return;
        }
        UUID clanId;
        try {
             clanId = UUID.fromString(args[0]);
        } catch (IllegalArgumentException ignored) {
            UtilMessage.message(player, "Logs", "<yellow>%s</yellow> is not a valid ID", args[0]);
            return;
        }
        new CachedLogMenu(args[0], LogContext.CLAN, clanId.toString(), null, CachedLogMenu.CLANS, JavaPlugin.getPlugin(Clans.class), logRepository, null).show(player);

    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ClanArgumentType.CLAN.name() : ArgumentType.NONE.name();
    }
}