package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.punishments.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

public class BanPunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "Ban";
    }

    @Override
    public String getChatLabel() {
        return "banned";
    }

    @Override
    public void onReceive(UUID client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client);
        if (player != null) {
            Component message = Component.text("You have been banned from the server").append(Component.newline()).append(Component.text("Reason: " + punishment.getReason()));
            player.kick(message, PlayerKickEvent.Cause.BANNED);
        }
    }
}
