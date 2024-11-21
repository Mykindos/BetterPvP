package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
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
        if (args.length < 4) {
            UtilMessage.message(sender, "Command", "Usage: /punish remove <type> <player> <revokeType> <reason>");
            return;
        }

        clientManager.search().offline(args[1], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                IPunishmentType type = PunishmentTypes.getPunishmentType(args[0]);
                RevokeType revokeType;
                try {
                    revokeType = RevokeType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    UtilMessage.message(sender, "Punish", "Invalid revoke type, must be APPEAL or INCORRECT");
                    return;
                }

                String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                List<Punishment> punishmentList = target.getPunishments().stream().filter(punishment -> punishment.isActive() && punishment.getType() == type).toList();
                if (punishmentList.isEmpty()) {
                    UtilMessage.message(sender, "Punish", "This client does not have any punishments of this type.");
                    return;
                } else {
                    UtilMessage.message(sender, "Punish", "Removed <green>%d</green> punishments from <yellow>%s</yellow>.", punishmentList.size(), target.getName());
                    if (sender instanceof Player player) {
                        log.info("{} had {} {} revoked by {}", target.getName(), punishmentList.size(), type.getName(), player.getName())
                                .setAction("PUNISH_REVOKE")
                                .addClientContext(target, true)
                                .addClientContext(player, false)
                                .submit();
                        clientManager.sendMessageToRank("Punish", UtilMessage.deserialize("<yellow>%s</yellow> revoked <green>%d</green> punishments from <yellow>%s</yellow>", player.getName(), punishmentList.size(), target.getName()), Rank.HELPER);
                    } else {
                        log.info("{} had {} {}s revoked", target.getName(), punishmentList.size(), type.getName())
                                .setAction("PUNISH_REVOKE")
                                .addClientContext(target, true)
                                .submit();
                    }

                }

                UUID revoker;
                if (sender instanceof Player player) {
                    revoker = player.getUniqueId();
                } else {
                    revoker = null;
                }

                punishmentList.forEach(punishment -> {
                    punishment.setRevoker(revoker);
                    punishment.setRevokeType(revokeType);
                    punishment.setRevokeTime(System.currentTimeMillis());
                    punishment.setRevokeReason(reason);
                    punishment.getType().onExpire(target, punishment);
                    punishmentRepository.revokePunishment(punishment);
                });
            } else {
                UtilMessage.message(sender, "Punish", "Could not find a client with this name.");
            }
        }, true);
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
            case "REVOKE" ->
                    tabCompletions.addAll(Arrays.stream(RevokeType.values()).map(revokeType -> revokeType.name().toLowerCase()).filter(name -> name.startsWith(lowercaseArg)).toList());
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
        } else if (i ==3) {
            return "REVOKE";
        }

        return ArgumentType.NONE.name();
    }

}
