package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MutePunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "Mute";
    }

    @Override
    public String getChatLabel() {
        return "muted";
    }

    @Override
    public void onExpire(Client client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client.getUniqueId());
        if (player != null) {
            UtilMessage.message(player, "Punish", "Your mute has expired");
        }
    }

}
