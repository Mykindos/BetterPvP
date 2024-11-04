package me.mykindos.betterpvp.core.client.punishments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Punishment {

    private final UUID id;
    private final UUID client;
    private final IPunishmentType type;
    private final long expiryTime;
    private final String reason;
    private final String punisher;
    private boolean revoked;

    public boolean hasExpired() {
        return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
    }

    public boolean isActive() {
        return !revoked && !hasExpired();
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
        Component currentComp;
        if (this.isActive()) {
            currentComp = Component.text("ACTIVE ", NamedTextColor.GREEN);
            if (expiryTime > 0) {
                currentComp = currentComp.append(Component.text(UtilTime.getTime((double) expiryTime - System.currentTimeMillis(), 1), NamedTextColor.RED));
            } else {
                currentComp = currentComp.append(Component.text("Permanent", NamedTextColor.RED));
            }

        } else if (revoked) {
            currentComp = Component.text("REVOKED", NamedTextColor.LIGHT_PURPLE);
        } else {
            currentComp = Component.text("INACTIVE", NamedTextColor.RED);
        }

        AtomicReference<String> punisherName = new AtomicReference<>("SERVER");
        if (punisher != null) {
            clientManager.search().offline(UUID.fromString(punisher), (clientOptional) -> {
                clientOptional.ifPresent(value -> punisherName.set(value.getName()));
            });
        }

        return Component.empty().append(currentComp).appendSpace()
                .append(Component.text(this.type.getName(), NamedTextColor.WHITE)).appendSpace()
                .append(Component.text(reason == null ? "No Reason" : reason, NamedTextColor.GRAY)).appendSpace()
                .append(Component.text(punisherName.get(), NamedTextColor.AQUA));
    }
}
