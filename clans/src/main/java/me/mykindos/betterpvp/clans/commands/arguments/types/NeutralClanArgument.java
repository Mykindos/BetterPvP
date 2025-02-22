package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.function.Predicate;

/**
 * Prompts the sender with a list of neutral Clans to the executor, guarantees a valid Clan return, but not a valid neutral
 */
@Singleton
public class NeutralClanArgument extends ClanArgument {
    @Inject
    protected NeutralClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Neutral Clan";
    }

    protected Predicate<Clan> executorClanPredicate(Clan executorClan) {
        return clan -> !executorClan.isAllied(clan) && !executorClan.isEnemy(clan) && !executorClan.equals(clan);
    }
}