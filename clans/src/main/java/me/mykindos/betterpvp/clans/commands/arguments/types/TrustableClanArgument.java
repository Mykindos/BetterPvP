package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.function.Predicate;

/**
 * Prompts the sender with a list of allied Clans to the executor, guarantees a valid Clan return, but not a valid ally
 */
@Singleton
public class TrustableClanArgument extends ClanArgument {
    @Inject
    protected TrustableClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Trustable Clan";
    }

    protected Predicate<Clan> executorClanPredicate(Clan executorClan) {
        return clan -> clanManager.canTrust(executorClan, clan);
    }

}
