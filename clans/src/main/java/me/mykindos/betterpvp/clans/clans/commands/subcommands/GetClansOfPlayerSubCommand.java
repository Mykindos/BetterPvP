package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.logging.ClanLogger;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@CustomLog
@Singleton
@SubCommand(ClanCommand.class)
public class GetClansOfPlayerSubCommand extends ClanSubCommand {

    private final ClanLogger clanLogger;

    @Inject
    public GetClansOfPlayerSubCommand(ClanManager clanManager, ClientManager clientManager, ClanLogger clanLogger) {
        super(clanManager, clientManager);
        this.clanLogger = clanLogger;
    }

    @Override
    public String getName() {
        return "playerclans";
    }

    @Override
    public String getDescription() {
        return "Get clans associated with a player";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length < 1) {
            UtilMessage.message(player, "Clan", getUsage());
            return;
        }

        clientManager.search().offline(args[0], clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, "Clan", "<green>%s</green> is not a valid player", args[0]);
                return;
            }
            Client client1 = clientOptional.get();


            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
                List<Component> components = clanLogger.getClansByPlayer(client1.getUniqueId());
                UtilMessage.message(player, "Clan", "Retrieving past and present Clans of <yellow>%s</yellow>", client1.getName());

                for (Component component : components) {
                    UtilMessage.message(player, component);
                }
            });
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
