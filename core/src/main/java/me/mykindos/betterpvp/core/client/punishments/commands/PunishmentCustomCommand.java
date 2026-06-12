package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Singleton
@CustomLog
@SubCommand(LegacyPunishmentCommand.class)
public class PunishmentCustomCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final RuleManager ruleManager;
    private final PunishmentRepository punishmentRepository;

    @Inject
    public PunishmentCustomCommand(ClientManager clientManager, RuleManager ruleManager, PunishmentRepository punishmentRepository) {
        this.clientManager = clientManager;
        this.ruleManager = ruleManager;
        this.punishmentRepository = punishmentRepository;
        aliases.add("a");
    }

    @Override
    public String getName() {
        return "custom";
    }

    @Override
    public String getDescription() {
        return "core.command.punishment-custom.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "core.prefix.command", "core.command.punishment.custom.usage.player");
            return;
        }

        clientManager.search().offline(args[1]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();
                if (target.getRank().getId() >= client.getRank().getId()) {
                    UtilMessage.message(player, "core.prefix.command", "core.command.punishment.rank.too_high");
                    return;
                }

                processPunishment(player, target, client, args);
            } else {
                UtilMessage.message(player, "core.prefix.command", "core.command.punishment.client.not_found");
            }
        }).exceptionally(ex -> {
            log.error("Error processing custom punishment", ex).submit();
            return null;
        });

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.custom.usage.console");
            return;
        }

        clientManager.search().offline(args[1]).thenAcceptAsync(clientOptional -> {
            if (clientOptional.isPresent()) {
                Client target = clientOptional.get();
                if (target.hasRank(Rank.ADMIN)) {
                    UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.rank.too_high");
                    return;
                }

                processPunishment(sender, target, null, args);
            } else {
                UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.client.not_found");
            }
        });
    }

    protected void processPunishment(CommandSender sender, Client target, Client punisher, String... args) {

        IPunishmentType type = PunishmentTypes.getPunishmentType(args[0]);
        if(type == null) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.type.invalid");
            return;
        }

        // Usage: /punish <type> <player> <time> <unit> <reason...>
        long time = 0;
        if (args[2].equalsIgnoreCase("perm")) {
            time = -1;
        } else {
            try {
                time = Long.parseLong(args[2]);
                if (type.hasDuration() && time < 0) {
                    UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.time.positive");
                    return;
                }
            } catch (NumberFormatException e) {
                UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.time.invalid");
                return;
            }
        }

        if (!type.hasDuration()) {
            time = 0;
        }

        String reason = "";
        if (time == -1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        } else {
            time = System.currentTimeMillis() + applyTimeUnit(time, args[3]);
            reason = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        }

        if (reason.isEmpty()) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.reason.required");
            return;
        }

        String formattedTime = new PrettyTime().format(new Date(time)).replace(" from now", "");

        Punishment punishment = new Punishment(SnowflakeIdGenerator.ID_GENERATOR.nextId(), target.getId(), target.getUniqueId(), type, ruleManager.getObject("CUSTOM").orElseThrow(), System.currentTimeMillis(), time, reason, punisher != null ? punisher.getUniqueId() : null);
        target.getPunishments().add(punishment);
        punishmentRepository.save(punishment);

        type.onReceive(target.getUniqueId(), punishment);
        if (punisher != null) {
            if (time == -1) {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.with_punisher.permanent", Component.text(punisher.getName()), Component.text(type.getChatLabel()), Component.text(target.getName()));
            } else {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.with_punisher.temp", Component.text(punisher.getName()), Component.text(type.getChatLabel()), Component.text(target.getName()), Component.text(formattedTime));
            }
            log.info("{} was {} by {} for {} reason {}", target.getName(), punisher.getName(), type.getChatLabel(), formattedTime, reason)
                    .setAction("PUNISH_ADD")
                    .addClientContext(target, true)
                    .addClientContext(punisher, false)
                    .submit();
        } else {
            if (time == -1) {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.no_punisher.permanent", Component.text(target.getName()), Component.text(type.getChatLabel()));
            } else {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.no_punisher.temp", Component.text(target.getName()), Component.text(type.getChatLabel()), Component.text(formattedTime));
            }
            log.info("{} was {} for {} reason {}", target.getName(), type.getChatLabel(), formattedTime, reason)
                    .setAction("PUNISH_ADD")
                    .addClientContext(target, true)
                    .submit();
        }

        if (!reason.isEmpty()) {
            Player targetPlayer = Bukkit.getPlayer(target.getUniqueId());
            Component reasonComponent = Translations.component("core.punishment.staff.reason",
                    Translations.component("core.punishment.staff.reason_label").color(NamedTextColor.RED),
                    Component.text(reason));

            if (targetPlayer != null) {
                UtilMessage.message(targetPlayer, "core.prefix.punish", reasonComponent);
            }

            clientManager.sendMessageToRank("Punish", reasonComponent, Rank.TRIAL_MOD);

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
