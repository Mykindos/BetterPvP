package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class PlayerTimeSubCommand extends ClanSubCommand {

    private final ClanLogger clanLogger;

    @Inject
    public PlayerTimeSubCommand(ClanManager clanManager, ClientManager clientManager, ClanLogger clanLogger) {
        super(clanManager, clientManager);
        this.clanLogger = clanLogger;
    }

    @Override
    public String getName() {
        return "playerclantime";
    }

    @Override
    public String getDescription() {
        return "Get the clan associated with the player at time";
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Clans", "You did not input a player to search.");
            return;
        }
        long time = 0;
        if (args.length >= 2) {
            try {
                time = Long.parseLong(args[1]);
            } catch (NumberFormatException ignored) {
                //pass
            }
        }
        long finalTime = time;
        clientManager.search().offline(args[0], client1 -> {
            if (client1.isEmpty()) {
                UtilMessage.message(player, "<yellow>%s</yellow> is not a valid username");
                return;
            }
            long newTime = System.currentTimeMillis() - finalTime;
            UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {
                Component clanComponent = clanLogger.getClanUUIDOfPlayerAtTime(UUID.fromString(client1.get().getUuid()), newTime);
                Component component = UtilMessage.deserialize("<yellow>%s</yellow> was in ", client1.get().getName())
                        .append(clanComponent).appendSpace()
                        .append(UtilMessage.deserialize("<green>%s</green> ago.", UtilTime.getTime(System.currentTimeMillis() - newTime, 2)));
                UtilMessage.message(player, "Clans", component);
            });
        });
    }
    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }
}