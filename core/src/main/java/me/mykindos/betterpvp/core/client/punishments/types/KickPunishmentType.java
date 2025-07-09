package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class KickPunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "Kick";
    }

    @Override
    public String getChatLabel() {
        return "kicked";
    }

    /**
     * Whether this punishment has a duration or not
     *
     * @return
     */
    @Override
    public boolean hasDuration() {
        return false;
    }

    @Override
    public void onReceive(UUID client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client);
        if (player != null) {
            UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                Component message = Component.text("You have been kicked from the server").append(Component.newline()).append(Component.text("Reason: " + punishment.getReason()));
                player.kick(message, PlayerKickEvent.Cause.KICK_COMMAND);
            });

        }
    }
}
