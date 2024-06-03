package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;

@Singleton
@CustomLog
@SubCommand(PunishCommand.class)
public class PunishmentHistoryCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;

    @Inject
    public PunishmentHistoryCommand(ClientManager clientManager, PunishmentRepository punishmentRepository) {
        this.clientManager = clientManager;
        aliases.add("h");
    }

    @Override
    public String getName() {
        return "history";
    }

    @Override
    public String getDescription() {
        return "Punish History Command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Punish", "Usage: /punish history <name>");
            return;
        }

        clientManager.search().offline(args[0], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                processHistory(player, target);
            } else {
                UtilMessage.message(player, "Punish", "Could not find a client with this name.");
            }
        });

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            UtilMessage.message(sender, "Punish", "Usage: /punish add <type> <player> <time> <unit> [reason...]");
            return;
        }

        clientManager.search().offline(args[0], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                processHistory(sender, target);
            } else {
                UtilMessage.message(sender, "Punish", "Could not find a client with this name.");
            }
        });
    }

    protected void processHistory(CommandSender sender, Client target) {
        UtilMessage.message(sender, "Punish", "Punishment History for <yellow>%s</yellow>", target.getName());
        target.getPunishments().sort(Comparator.comparingLong(Punishment::getExpiryTime).reversed());
        target.getPunishments().forEach(punishment -> {
            UtilMessage.message(sender, "", punishment.getPunishmentInformation());
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
