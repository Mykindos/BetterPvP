package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.*;

@Singleton
@SubCommand(PunishCommand.class)
public class PunishmentAddCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final PunishmentRepository punishmentRepository;

    @Inject
    public PunishmentAddCommand(ClientManager clientManager, PunishmentRepository punishmentRepository) {
        this.clientManager = clientManager;
        this.punishmentRepository = punishmentRepository;
        aliases.add("a");
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getDescription() {
        return "Base punishment command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "Command", "Usage: /punish add <type> <player> <time> [unit] [reason...]");
            return;
        }

        clientManager.search().offline(args[1], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();
                if (target.getRank().getId() >= client.getRank().getId()) {
                    UtilMessage.message(player, "Punish", "You cannot punish a client with the same or higher rank.");
                    return;
                }

                processPunishment(player, target, client, args);
            } else {
                UtilMessage.message(player, "Punish", "Could not find a client with this name.");
            }
        });

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            UtilMessage.message(sender, "Command", "Usage: /punish add <type> <player> <time> [unit] [reason...]");
            return;
        }

        clientManager.search().offline(args[1], clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();
                if (target.hasRank(Rank.ADMIN)) {
                    UtilMessage.message(sender, "Punish", "You cannot punish this client.");
                    return;
                }

                processPunishment(sender, target, null, args);
            } else {
                UtilMessage.message(sender, "Punish", "Could not find a client with this name.");
            }
        });
    }

    protected void processPunishment(CommandSender sender, Client target, Client punisher, String... args) {

        IPunishmentType type = PunishmentTypes.getPunishmentType(args[0]);
        if(type == null) {
            UtilMessage.message(sender, "Punish", "Invalid punishment type.");
            return;
        }

        // Usage: /punish <type> <player> <time> <unit> <reason...>
        long time = 0;
        if (args[2].equalsIgnoreCase("perm")) {
            time = -1;
        } else {
            try {
                time = Long.parseLong(args[2]);
                if(time < 0) {
                    UtilMessage.message(sender, "Punish", "Time must be greater than 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                UtilMessage.message(sender, "Punish", "Invalid time format, must be a number or 'perm'.");
                return;
            }
        }

        String reason = "";
        if (time == -1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        } else {
            time = System.currentTimeMillis() + applyTimeUnit(time, args[3]);
            reason = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        }

        String formattedTime = new PrettyTime().format(new Date(time)).replace(" from now", "");

        Punishment punishment = new Punishment(UUID.randomUUID(), target.getUniqueId(), type, time, reason, punisher != null ? punisher.getUniqueId().toString() : null);
        target.getPunishments().add(punishment);
        punishmentRepository.save(punishment);

        type.onReceive(target, punishment);
        if (punisher != null) {
            if (time == -1) {
                UtilMessage.broadcast("Punish", "<yellow>%s<reset> has <green>permanently <reset>%s <yellow>%s<reset>.", punisher.getName(), type.getChatLabel(), target.getName());
            } else {
                UtilMessage.broadcast("Punish", "<yellow>%s<reset> has %s <yellow>%s<reset> for <green>%s<reset>.", punisher.getName(), type.getChatLabel(), target.getName(), formattedTime);
            }
        } else {
            if (time == -1) {
                UtilMessage.broadcast("Punish", "<yellow>%s<reset> was <green>permanently <reset>%s.", target.getName(), type.getChatLabel());
            } else {
                UtilMessage.broadcast("Punish", "<yellow>%s<reset> was %s for <green>%s<reset>.", target.getName(), type.getChatLabel(), formattedTime);
            }
        }

        if (!reason.isEmpty()) {
            UtilMessage.broadcast("Punish", "<red>Reason<reset>: <reset>%s", reason);
        }

    }

    private long applyTimeUnit(long time, String unit) {
        return switch (unit) {
            case "s", "seconds" -> time * 1000;
            case "m", "minutes" -> time * 1000 * 60;
            case "h", "hours" -> time * 1000 * 60 * 60;
            case "d", "days" -> time * 1000 * 60 * 60 * 24;
            case "w", "weeks" -> time * 1000 * 60 * 60 * 24 * 7;
            case "mo", "months" -> time * 1000 * 60 * 60 * 24 * 30;
            case "y", "years" -> time * 1000 * 60 * 60 * 24 * 365;
            default -> time;
        };
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
