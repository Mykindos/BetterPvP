package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BuildLockPunishmentType implements IPunishmentType {

    @Override
    public String getName() {
        return "BuildLock";
    }

    @Override
    public String getChatLabel() {
        return "build locked";
    }

    @Override
    public void onReceive(Client client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client.getUniqueId());
        if (player != null) {
            UtilMessage.message(player, "Punish", "You have been build locked for");
        }
    }

    @Override
    public void onExpire(Client client, Punishment punishment) {
        Player player = Bukkit.getPlayer(client.getUniqueId());
        if (player != null) {
            UtilMessage.message(player, "Punish", "Your build lock punishment has expired");
        }
    }
}
