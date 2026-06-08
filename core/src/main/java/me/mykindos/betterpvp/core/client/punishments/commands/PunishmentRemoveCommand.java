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
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
@SubCommand(LegacyPunishmentCommand.class)
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
        return "core.command.punishment-remove.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.remove.usage");
            return;
        }

        clientManager.search().offline(args[1]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();

                IPunishmentType type = PunishmentTypes.getPunishmentType(args[0]);
                RevokeType revokeType;
                try {
                    revokeType = RevokeType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.remove.revoke_type.invalid");
                    return;
                }

                String reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                List<Punishment> punishmentList = target.getPunishments().stream().filter(punishment -> punishment.isActive() && punishment.getType() == type).toList();
                if (punishmentList.isEmpty()) {
                    UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.remove.none_of_type");
                    return;
                } else {
                    UtilMessage.message(sender, "core.prefix.command", Translations.component("core.command.punishment.remove.success",
                            Component.text(punishmentList.size()), Component.text(target.getName())));
                    if (sender instanceof Player player) {
                        log.info("{} had {} {} revoked by {}", target.getName(), punishmentList.size(), type.getName(), player.getName())
                                .setAction("PUNISH_REVOKE")
                                .addClientContext(target, true)
                                .addClientContext(player, false)
                                .submit();
                        clientManager.sendMessageToRank("core.prefix.command",
                                Translations.component("core.command.punishment.remove.staff_broadcast",
                                        Component.text(player.getName()), Component.text(punishmentList.size()), Component.text(target.getName())),
                                Rank.TRIAL_MOD);
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
                    punishment.setRevoked(revoker, revokeType, reason);
                    punishmentRepository.revokePunishment(punishment);
                });
            } else {
                UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.client.not_found");
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
