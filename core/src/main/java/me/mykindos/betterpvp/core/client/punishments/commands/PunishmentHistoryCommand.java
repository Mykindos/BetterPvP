package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentHandler;
import me.mykindos.betterpvp.core.client.punishments.menu.PunishmentItem;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;

@Singleton
@CustomLog
@SubCommand(LegacyPunishmentCommand.class)
public class PunishmentHistoryCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final PunishmentHandler punishmentHandler;

    @Inject
    public PunishmentHistoryCommand(ClientManager clientManager, PunishmentHandler punishmentHandler) {
        this.clientManager = clientManager;
        this.punishmentHandler = punishmentHandler;
        aliases.add("h");
    }

    @Override
    public String getName() {
        return "history";
    }

    @Override
    public String getDescription() {
        return "core.command.punishment-history.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.command", "core.command.punishment.history.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();
                processHistory(player, target);

                List<Item> items = target.getPunishments().stream()
                        .sorted(Comparator.comparingLong(Punishment::getApplyTime).reversed())
                        .sorted(Comparator.comparing(Punishment::isActive).reversed())
                        .map(punishment -> new PunishmentItem(
                                punishment,
                                punishmentHandler,
                                punishmentHandler.getClientManager().search().offline(punishment.getPunisher()).join().map(Client::getName).orElse(null),
                                punishmentHandler.getClientManager().search().offline(punishment.getRevoker()).join().map(Client::getName).orElse(null),
                                true,
                                punishment.getRevokeReason(),
                                null))
                        .map(Item.class::cast).toList();
                // Note: Menu title currently requires a String; leaving literal until API accepts translated Components
                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> new ViewCollectionMenu(target.getName() + "'s Punish History", items, null).show(player));

            } else {
                UtilMessage.message(player, "core.prefix.command", "core.command.punishment.client.not_found");
            }
        });
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.history.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                processHistory(sender, target);
            } else {
                UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.client.not_found");
            }
        });
    }

    protected void processHistory(CommandSender sender, Client target) {
        UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.history.header", net.kyori.adventure.text.Component.text(target.getName()));
        target.getPunishments().sort(Comparator.comparingLong(Punishment::getExpiryTime).reversed());
        target.getPunishments().forEach(punishment -> {
            UtilMessage.message(sender, "", punishment.getPunishmentInformation(clientManager));
        });
    }

    @Override
    public String getArgumentType(int i) {
        if (i == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }
}
