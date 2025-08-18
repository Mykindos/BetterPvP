package me.mykindos.betterpvp.core.chat.ignore.impl;

import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;

/**
 * The DefaultIgnoreService class implements the IIgnoreService interface and provides
 * a default implementation to check if one client has ignored another client.
 * The implementation relies on locally stored ignore data in the source client.
 */
public class DefaultIgnoreService implements IIgnoreService {

    /**
     * Checks whether the target client is ignored by the source client.
     *
     * @param source The client that may have ignored the target client.
     * @param target The client that may be ignored by the source client.
     * @return true if the source client has ignored the target client, false otherwise.
     */
    @Override
    public boolean isClientIgnored(Client source, Client target) {
        return source.getIgnores().contains(target.getUniqueId()) && !target.hasRank(Rank.TRIAL_MOD);
    }

}
