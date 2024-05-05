package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.Punishment;

public interface IPunishmentType {

    String getName();
    String getChatLabel();

    default void onReceive(Client client, Punishment punishment) {}

    default void onExpire(Client client, Punishment punishment) {}

}
