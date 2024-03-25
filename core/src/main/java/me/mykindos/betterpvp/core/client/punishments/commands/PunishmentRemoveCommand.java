package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
@SubCommand(PunishCommand.class)
public class PunishmentRemoveCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final PunishmentRepository punishmentRepository;

    @Inject
    public PunishmentRemoveCommand(ClientManager clientManager, PunishmentRepository punishmentRepository) {
        this.clientManager = clientManager;
        this.punishmentRepository = punishmentRepository;

        aliases.add("r");
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Base command for removing a punishment from a client";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            UtilMessage.message(sender, "Command", "Usage: /punish remove <player> <type>");
            return;
        }

        clientManager.search().offline(args[1], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                List<Punishment> punishmentList = target.getPunishments().stream().filter(punishment -> punishment.isActive() && punishment.getType().getName().equalsIgnoreCase(args[0])).toList();
                if (punishmentList.isEmpty()) {
                    UtilMessage.message(sender, "Punish", "This client does not have any punishments of this type.");
                    return;
                } else {
                    UtilMessage.message(sender, "Punish", "Removed <green>%d</green> punishments from <yellow>%s</yellow>.", punishmentList.size(), target.getName());
                }

                punishmentList.forEach(punishment -> {
                    punishment.setRevoked(true);
                    punishmentRepository.revokePunishment(punishment);
                });
            } else {
                UtilMessage.message(sender, "Punish", "Could not find a client with this name.");
            }
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 0) return tabCompletions;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        switch (getArgumentType(args.length)) {
            case "PLAYER" ->
                    tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                            startsWith(lowercaseArg)).toList());
            case "TYPE" ->
                    tabCompletions.addAll(PunishmentTypes.getPunishmentTypes().stream().map(punishmentType -> punishmentType.getName().toLowerCase()).filter(name -> name.startsWith(lowercaseArg)).toList());
            default -> {
                return tabCompletions;
            }
        }

        return tabCompletions;
    }

    @Override
    public String getArgumentType(int i) {

        if (i == 1) {
            return "TYPE";
        } else if (i == 2) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }

}
