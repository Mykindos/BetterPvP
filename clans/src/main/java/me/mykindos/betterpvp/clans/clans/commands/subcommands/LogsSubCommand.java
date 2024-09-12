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

@Singleton
@SubCommand(ClanCommand.class)
public class LogsSubCommand extends ClanSubCommand {

    private final LogRepository logRepository;

    @Inject
    public LogsSubCommand(ClanManager clanManager, ClientManager clientManager, LogRepository logRepository) {
        super(clanManager, clientManager);
        this.logRepository = logRepository;
    }

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public String getDescription() {
        return "Get logs associated with your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, "Clans", "You must be in a Clan to run this command");
            return;
        }

        new CachedLogMenu(clan.getName(), LogContext.CLAN, clan.getId().toString(), null, CachedLogMenu.CLANS, JavaPlugin.getPlugin(Clans.class), logRepository, null).show(player);

    }
}