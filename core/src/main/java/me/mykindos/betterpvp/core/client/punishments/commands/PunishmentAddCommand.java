package me.mykindos.betterpvp.core.client.punishments.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
@CustomLog
@SubCommand(LegacyPunishmentCommand.class)
public class PunishmentAddCommand extends Command implements IConsoleCommand {

    private final ClientManager clientManager;
    private final RuleManager ruleManager;
    private final PunishmentRepository punishmentRepository;

    @Inject
    public PunishmentAddCommand(ClientManager clientManager, RuleManager ruleManager, PunishmentRepository punishmentRepository) {
        this.clientManager = clientManager;
        this.ruleManager = ruleManager;
        this.punishmentRepository = punishmentRepository;
        aliases.add("a");
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getDescription() {
        return "core.command.punishment-add.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "core.prefix.command", "core.command.punishment.add.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
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
        });

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.add.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync(clientOptional -> {
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

        Optional<Rule> ruleOptional = ruleManager.getObject(args[1].toLowerCase());

        if (ruleOptional.isEmpty()) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.rule.invalid");
            return;
        }

        Rule rule = ruleOptional.get();

        if (rule.getKey().equalsIgnoreCase("custom")) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.rule.reserved");
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (reason.isEmpty()) {
            UtilMessage.message(sender, "core.prefix.command", "core.command.punishment.reason.required");
            return;
        }

        KeyValue<IPunishmentType, Long> punishInfo = rule.getPunishmentForClient(target);
        IPunishmentType type = punishInfo.getKey();
        long time = punishInfo.getValue();

        String formattedTime = UtilTime.getTime(time, 1);

        Punishment punishment = new Punishment(
                SnowflakeIdGenerator.ID_GENERATOR.nextId(),
                target.getId(),
                target.getUniqueId(),
                type,
                rule,
                System.currentTimeMillis(),
                time < 0 ? -1 : System.currentTimeMillis() + time,
                reason,
                punisher != null ? punisher.getUniqueId() : null
        );
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

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 0) return tabCompletions;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        switch (getArgumentType(args.length)) {
            case "PLAYER" ->
                    tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                            startsWith(lowercaseArg)).toList());
            case "RULE" ->
                    tabCompletions.addAll(ruleManager.getObjects().values().stream().map(rule -> rule.getKey().toLowerCase().replace(' ', '_')).filter(name -> name.startsWith(lowercaseArg)).toList());
            default -> {
                return tabCompletions;
            }
        }

        return tabCompletions;
    }

    @Override
    public String getArgumentType(int i) {

        if (i == 1) {
            return ArgumentType.PLAYER.name();
        } else if (i == 2) {
            return "RULE";
        }

        return ArgumentType.NONE.name();
    }
}
