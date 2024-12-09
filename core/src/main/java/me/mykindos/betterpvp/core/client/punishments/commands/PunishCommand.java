package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.client.punishments.menu.PunishmentMenu;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

@Singleton
public class PunishCommand extends Command {

    private final ClientManager clientManager;
    private final PunishmentHandler punishmentHandler;

    @Inject
    public PunishCommand(ClientManager clientManager, PunishmentHandler punishmentHandler) {
        this.clientManager = clientManager;
        this.punishmentHandler = punishmentHandler;
    }

    @Override
    public String getName() {
        return "punish";
    }

    @Override
    public String getDescription() {
        return "Base command for punishing system";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "Punish", "Usage: /punish <player> <reason...>");
            return;
        }
        clientManager.search().offline(args[0], clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, "Punish", "Could not find a client with the name <yellow>%s</yellow>", args[0]);
                return;
            }
            Client target = clientOptional.get();
            if (target.getRank().getId() >= client.getRank().getId()) {
                UtilMessage.message(player, "Punish", "You cannot punish a client with the same or higher rank.");
                return;
            }
            PunishmentMenu punishmentMenu = new PunishmentMenu(client,
                    target,
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)),
                    punishmentHandler,
                    null);
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                punishmentMenu.show(player);
            });
        }, true);
    }

    @Override
    public String getArgumentType(int argCount) {
            return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
