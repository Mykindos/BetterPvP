package me.mykindos.betterpvp.core.client.punishments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.punishments.types.IPunishmentType;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.UUID;

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
    public Component getPunishmentInformation() {
        Component currentComp;
        if (this.isActive()) {
            currentComp = Component.text("ACTIVE", NamedTextColor.GREEN);
        } else if (revoked) {
            currentComp = Component.text("REVOKED", NamedTextColor.LIGHT_PURPLE);
        } else {
            currentComp = Component.text("INACTIVE", NamedTextColor.RED);
        }

        Component component = Component.empty().append(currentComp).appendSpace()
                .append(Component.text(this.type.getName(), NamedTextColor.WHITE)).appendSpace()
                .append(Component.text(reason == null ? "No Reason" : reason, NamedTextColor.GRAY)).appendSpace()
                .append(Component.text(punisher == null ? "SERVER" : Bukkit.getOfflinePlayer(UUID.fromString(punisher)).getName(), NamedTextColor.AQUA));
        return component;
    }
}
