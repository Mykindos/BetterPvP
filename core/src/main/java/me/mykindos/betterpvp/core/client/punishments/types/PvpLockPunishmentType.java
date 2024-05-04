package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PvpLockPunishmentType implements IPunishmentType {
    @Override
    public String getName() {
        return "PVPLock";
    }

    @Override
    public String getChatLabel() {
        return "PVP locked";
    }

    @Override
    public void onExpire(Client client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client.getUniqueId());
        if (player != null) {
            UtilMessage.message(player, "Punish", "Your pvp lock punishment has expired");
        }
    }

}
