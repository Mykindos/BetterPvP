package me.mykindos.betterpvp.core.chat.ignore.impl;

import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.ignore.PlayerIgnoreModule;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;

/**
 * The MineplexIgnoreService class implements the IIgnoreService interface and provides functionality
 * for checking if one client has ignored another client. The logic relies on the PlayerIgnoreModule,
 * which is a registered module in the MineplexModuleManager.
 */
public class MineplexIgnoreService implements IIgnoreService {

    private final PlayerIgnoreModule ignoreModule;

    public MineplexIgnoreService() {
        this.ignoreModule = MineplexModuleManager.getRegisteredModule(PlayerIgnoreModule.class);
    }

    /**
     * Determines if the target client is ignored by the source client.
     *
     * @param source The client who might be ignoring the target.
     * @param target The client who might be ignored by the source.
     * @return true if the source client has ignored the target client; false otherwise.
     */
    @Override
    public boolean isClientIgnored(Client source, Client target) {
        return ignoreModule.isIgnored(source.getUniqueId(), target.getUniqueId()) && !target.hasRank(Rank.TRIAL_MOD);
    }

}
