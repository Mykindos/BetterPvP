package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessage;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesHandler;
import me.mykindos.betterpvp.core.client.punishments.menu.ApplyPunishmentItem;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class PunishmentHandler {
    private final PunishmentRepository punishmentRepository;
    @Getter
    private final RuleManager ruleManager;
    @Getter
    private final ClientManager clientManager;
    private final OfflineMessagesHandler offlineMessagesHandler;

    @Inject
    public PunishmentHandler(PunishmentRepository punishmentRepository, RuleManager ruleManager, ClientManager clientManager, OfflineMessagesHandler offlineMessagesHandler) {
        this.punishmentRepository = punishmentRepository;
        this.ruleManager = ruleManager;
        this.clientManager = clientManager;
        this.offlineMessagesHandler = offlineMessagesHandler;
    }

    public List<Item> getApplyPunishmentItemList(Client punisher, String category, Client target, String reason, Windowed previous) {
        return getRuleManager().getObjects().values().stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase(category))
                .map(rule -> new ApplyPunishmentItem(punisher, rule, target, reason, this, previous))
                .map(Item.class::cast)
                .toList();
    }

    public void punish(Client punisher, Client target, String reason, Rule rule) {
        KeyValue<IPunishmentType, Long> punishInfo = rule.getPunishmentForClient(target);
        applyPunishment(punisher, target, reason, punishInfo.getKey(), punishInfo.getValue(), rule);
    }

    public void punish(Client target, String reason, IPunishmentType type, long duration) {
        applyPunishment(null, target, reason, type, duration, ruleManager.getObject("CUSTOM").orElseThrow());
    }

    public void punish(@Nullable Client punisher, Client target, String reason, IPunishmentType type, long duration) {
        applyPunishment(punisher, target, reason, type, duration, ruleManager.getObject("CUSTOM").orElseThrow());
    }

    private void applyPunishment(@Nullable Client punisher, Client target, String reason, IPunishmentType type, long duration, Rule rule) {
        long expiryTime = duration < 0 ? -1 : System.currentTimeMillis() + duration;
        String formattedTime = UtilTime.getTime(duration, 1);

        Punishment punishment = new Punishment(
                SnowflakeIdGenerator.ID_GENERATOR.nextId(),
                target.getId(),
                target.getUniqueId(),
                type,
                rule,
                System.currentTimeMillis(),
                expiryTime,
                reason,
                punisher != null ? punisher.getUniqueId() : null
        );
        target.getPunishments().add(punishment);
        punishmentRepository.save(punishment);
        type.onReceive(target.getUniqueId(), punishment);

        Component staffPunishMessage;
        if (duration == -1) {
            if (punisher != null) {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.with_punisher.permanent",
                        Component.text(punisher.getName()), Component.text(type.getChatLabel()), Component.text(target.getName()));
            } else {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.no_punisher.permanent",
                        Component.text(target.getName()), Component.text(type.getChatLabel()));
            }
            staffPunishMessage = Translations.component("core.punishment.staff.permanent",
                    Component.text(target.getName(), NamedTextColor.YELLOW),
                    Component.text("permanently", NamedTextColor.GREEN),
                    Component.text(type.getChatLabel()),
                    Component.text(punisher != null ? punisher.getName() : "Server", NamedTextColor.YELLOW));
            offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(),
                    OfflineMessage.Action.PUNISHMENT,
                    "You were <green>permanently</green> <yellow>%s</yellow>. Reason: <red>%s",
                    type.getChatLabel(), punishment.getReason());
        } else if (duration == 0) {
            // Treat 0 duration as a temporary punishment with a 0s duration for broadcast consistency
            String zero = UtilTime.getTime(0, 1);
            if (punisher != null) {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.with_punisher.temp",
                        Component.text(punisher.getName()), Component.text(type.getChatLabel()), Component.text(target.getName()), Component.text(zero));
            } else {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.no_punisher.temp",
                        Component.text(target.getName()), Component.text(type.getChatLabel()), Component.text(zero));
            }
            staffPunishMessage = Translations.component("core.punishment.staff.applied",
                    Component.text(target.getName(), NamedTextColor.YELLOW),
                    Component.text(type.getChatLabel()),
                    Component.text(punisher != null ? punisher.getName() : "Server", NamedTextColor.YELLOW));
            offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(),
                    OfflineMessage.Action.PUNISHMENT,
                    "You were <yellow>%s</yellow>. Reason: <red>%s",
                    type.getChatLabel(), punishment.getReason());
        } else {
            if (punisher != null) {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.with_punisher.temp",
                        Component.text(punisher.getName()), Component.text(type.getChatLabel()), Component.text(target.getName()), Component.text(formattedTime));
            } else {
                UtilMessage.broadcast("core.prefix.command", "core.command.punishment.broadcast.no_punisher.temp",
                        Component.text(target.getName()), Component.text(type.getChatLabel()), Component.text(formattedTime));
            }
            staffPunishMessage = Translations.component("core.punishment.staff.temp",
                    Component.text(target.getName(), NamedTextColor.YELLOW),
                    Component.text(type.getChatLabel()),
                    Component.text(formattedTime, NamedTextColor.GREEN),
                    Component.text(punisher != null ? punisher.getName() : "Server", NamedTextColor.YELLOW));
            offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(),
                    OfflineMessage.Action.PUNISHMENT,
                    "You were <yellow>%s</yellow> for <green>%s</green>. Reason: <red>%s",
                    type.getChatLabel(), formattedTime, punishment.getReason());
        }
        clientManager.sendMessageToRank("Punish", staffPunishMessage, Rank.TRIAL_MOD);

        if (punisher != null) {
            log.info("{} was {} by {} for {} reason {}", target.getName(), type.getChatLabel(), punisher.getName(), formattedTime, reason)
                    .setAction("PUNISH_ADD")
                    .addClientContext(target, true)
                    .addClientContext(punisher, false)
                    .submit();
        } else {
            log.info("{} was {} by Server for {} reason {}", target.getName(), type.getChatLabel(), formattedTime, reason)
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

    public void revoke(Punishment punishment, UUID revoker, RevokeType revokeType, String reason) {
        punishment.setRevoked(revoker, revokeType, reason);
        punishmentRepository.revokePunishment(punishment);
    }


}
