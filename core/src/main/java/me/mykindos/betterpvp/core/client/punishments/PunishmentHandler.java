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
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
        IPunishmentType type = punishInfo.getKey();
        long time = punishInfo.getValue();

        String formattedTime = UtilTime.getTime(time, 1);

        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                target.getUniqueId(),
                type,
                rule,
                System.currentTimeMillis(),
                time < 0 ? -1 : System.currentTimeMillis() + time,
                reason,
                punisher.getUniqueId()
        );
        target.getPunishments().add(punishment);
        punishmentRepository.save(punishment);
        type.onReceive(target.getUniqueId(), punishment);

        if (time == -1) {
            UtilMessage.broadcast("Punish", "<yellow>%s<reset> has <green>permanently <reset>%s <yellow>%s<reset>.", punisher.getName(), type.getChatLabel(), target.getName());
            offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(),
                    OfflineMessage.Action.PUNISHMENT,
                    "You were <green>permanently</green> <yellow>%s</yellow>. Reason: <red>%s",
                    type.getChatLabel(), punishment.getReason());
        } else {
            UtilMessage.broadcast("Punish", "<yellow>%s<reset> has %s <yellow>%s<reset> for <green>%s<reset>.", punisher.getName(), type.getChatLabel(), target.getName(), formattedTime);
            offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(),
                    OfflineMessage.Action.PUNISHMENT,
                    "You were <yellow>%s</yellow> for <green>%s</green>. Reason: <red>%s",
                    type.getChatLabel(), formattedTime, punishment.getReason());
        }
        log.info("{} was {} by {} for {} reason {}", target.getName(), punisher.getName(), type.getChatLabel(), formattedTime, reason)
                .setAction("PUNISH_ADD")
                .addClientContext(target, true)
                .addClientContext(punisher, false)
                .submit();

        if (!reason.isEmpty()) {
            Player targetPlayer = Bukkit.getPlayer(target.getUniqueId());
            Component reasonComponent = UtilMessage.deserialize("<red>Reason<reset>: <reset>%s", reason);

            if (targetPlayer != null) {
                UtilMessage.message(targetPlayer, "Punish", reasonComponent);
            }

            clientManager.sendMessageToRank("Punish", reasonComponent, Rank.TRIAL_MOD);
        }
    }

    public void revoke(Punishment punishment, UUID revoker, RevokeType revokeType, String reason) {
        punishment.setRevoked(revoker, revokeType, reason);
        punishmentRepository.revokePunishment(punishment);
    }


}
