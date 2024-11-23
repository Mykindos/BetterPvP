package me.mykindos.betterpvp.core.client.punishments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.punishments.rules.Rule;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.punishments.types.RevokeType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Punishment {

    /**
     * The id of this punishment
     */
    private final UUID id;
    /**
     * The UUID of the punished player
     */
    private final UUID client;
    /**
     * The type of punishment
     */
    private final IPunishmentType type;
    /**
     * The rule this punishment si for
     */
    private final Rule rule;
    /**
     * When this punishment was applied
     */
    private final long applyTime;
    /**
     * When this punishment should expire
     */
    private final long expiryTime;
    /**
     * Why this punishment was applied
     */
    private final String reason;
    /**
     * The UUID of the punisher
     */
    @Nullable(value = "Null if server punish")
    private final UUID punisher;
    /**
     * The UUID of the revoker
     */
    @Nullable(value = "Null if not revoked")
    private UUID revoker;
    /**
     * The type of revoke
     */
    @Nullable(value = "Null if not revoked")
    private RevokeType revokeType;
    /**
     * The time of the revoke.
     * @value -1 = not revoked
     */
    private long revokeTime = -1;
    /**
     * The revoke reason
     */
    @Nullable(value = "Null if not revoked")
    private String revokeReason;

    /**
     * See if this punishment has expired
     * @return true if expired
     */
    public boolean hasExpired() {
        return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
    }

    /**
     * See if this punishment is revoked
     * @return true if revoked
     */
    public boolean isRevoked() {
        return revokeTime != -1 && System.currentTimeMillis() > revokeTime;
    }

    /**
     * See if this punishment is active. Active means it is not revoked and not expired.
     * @return true if active
     */
    public boolean isActive() {
        return !isRevoked() && !hasExpired();
    }

    /**
     * @return The formatted information about the punishment
     */
    public Component getInformation() {
        if (!this.isActive()) {
            return UtilMessage.deserialize("You are no longer <red>%s</red>", this.type.getChatLabel());
        }
        String formattedTime = new PrettyTime().format(new Date(this.getExpiryTime())).replace(" from now", "");
        return UtilMessage.deserialize("You are <red>%s</red> for <green>%s</green> for <yellow>%s</yellow>", this.type.getChatLabel(), formattedTime, this.getReason());
    }

    /**
     * @return the formatted punishment information
     */
    public Component getPunishmentInformation(ClientManager clientManager) {
        AtomicReference<String> punisherName = new AtomicReference<>("SERVER");
        if (punisher != null) {
            clientManager.search().offline(punisher, (clientOptional) -> {
                clientOptional.ifPresent(value -> punisherName.set(value.getName()));
            }, false);
        }

        AtomicReference<String> revokerName = new AtomicReference<>("SERVER");
        if (revoker != null) {
            clientManager.search().offline(revoker, (clientOptional) -> {
                clientOptional.ifPresent(value -> revokerName.set(value.getName()));
            }, false);
        }

        Component currentComp = Component.empty();
        if (this.isActive()) {
            currentComp = currentComp.append(Component.text("A ", NamedTextColor.GREEN));
            if (expiryTime > 0) {
                currentComp = currentComp.append(UtilMessage.deserialize("<yellow>%s</yellow> ago <green>%s</green> (<red>%s</red>)",
                                        UtilTime.getTime(System.currentTimeMillis() - applyTime, 1),
                                        UtilTime.getTime(expiryTime - applyTime, 1),
                                        UtilTime.getTime((double) expiryTime - System.currentTimeMillis(), 1)));
            } else {
                currentComp = currentComp.append(UtilMessage.deserialize("<yellow>%s</yellow> ago <green>%s</green> (<red>%s</red>)",
                        UtilTime.getTime(System.currentTimeMillis() - applyTime, 1),
                        "PERM",
                        "n/a"));
            }

        } else if (isRevoked()) {
            currentComp = currentComp.append(Component.text("R ", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(revokerName.get(), NamedTextColor.LIGHT_PURPLE))
                            .hoverEvent(HoverEvent.showText(UtilMessage.deserialize("Time: <green>%s</green> ago Type: <yellow>%s</yellow> Reason: <white>%s</white>",
                                    UtilTime.getTime(System.currentTimeMillis() - revokeTime, 1),
                                    revokeType != null ? revokeType.name() : null,
                                    revokeReason)));
        } else {
            currentComp = currentComp.append(Component.text("E ", NamedTextColor.RED)).append(UtilMessage.deserialize("<yellow>%s</yellow> ago <green>%s</green> (<red>%s</red>)",
                    UtilTime.getTime(System.currentTimeMillis() - applyTime, 1),
                    UtilTime.getTime(expiryTime - applyTime, 1),
                    "n/a", 1));
        }
        return Component.empty().append(currentComp).appendSpace()
                .append(Component.text(this.type.getName(), NamedTextColor.WHITE)).appendSpace()
                .append(Component.text(rule.getKey(), NamedTextColor.YELLOW)).appendSpace()
                .append(Component.text(reason == null ? "No Reason" : reason, NamedTextColor.GRAY)).appendSpace()
                .append(Component.text(punisherName.get(), NamedTextColor.AQUA));
    }
}
