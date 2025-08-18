package me.mykindos.betterpvp.core.chat.ignore;

import me.mykindos.betterpvp.core.client.Client;

public interface IIgnoreService {

    /**
     * Determines if the target client is ignored by the source client.
     *
     * @param source The client who might have ignored the target.
     * @param target The client who might be ignored by the source.
     * @return true if the source client has ignored the target client; false otherwise.
     */
    boolean isClientIgnored(Client source, Client target);

}
