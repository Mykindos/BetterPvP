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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Arrays;

@Singleton
public class PunishCommand extends Command {

    private final Core core;
    private final ClientManager clientManager;
    private final PunishmentHandler punishmentHandler;

    @Inject
    public PunishCommand(Core core, ClientManager clientManager, PunishmentHandler punishmentHandler) {
        this.core = core;
        this.clientManager = clientManager;
        this.punishmentHandler = punishmentHandler;
    }

    @Override
    public String getName() {
        return "punish";
    }

    @Override
    public String getDescription() {
        return "core.command.punish.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.command", "core.command.punish.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isEmpty()) {
                UtilMessage.message(player, "core.prefix.command", "core.command.punish.client.not_found",
                        net.kyori.adventure.text.Component.text(args[0], NamedTextColor.YELLOW));
                return;
            }
            Client target = clientOptional.get();
            if (target.getRank().getId() >= client.getRank().getId()) {
                UtilMessage.message(player, "core.prefix.command", "core.command.punish.rank.too_high");
                return;
            }

            PunishmentMenu punishmentMenu = new PunishmentMenu(client,
                    target,
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)),
                    punishmentHandler,
                    null);
            UtilServer.runTask(core, () -> punishmentMenu.show(player));
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
