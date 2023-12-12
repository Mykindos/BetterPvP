package me.mykindos.betterpvp.core.client.exception;

import org.bukkit.entity.Player;

public class ClientNotLoadedException extends RuntimeException {

    public ClientNotLoadedException(Player player) {
        super(String.format("Player was online but client is not currently loaded in storage. They were kicked from the server (Name: %s | UUID: %s)", player.getName(), player.getUniqueId()));
    }

}
