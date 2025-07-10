package me.mykindos.betterpvp.core.client.punishments.types;

import me.mykindos.betterpvp.core.client.punishments.Punishment;

import java.util.UUID;

public interface IPunishmentType {

    String getName();
    String getChatLabel();

    /**
     * Whether this punishment has a duration or not
     * @return
     */
    default boolean hasDuration() {
        return true;
    }

    default void onReceive(UUID client, Punishment punishment) {}

    default void onExpire(UUID client, Punishment punishment) {}

}
