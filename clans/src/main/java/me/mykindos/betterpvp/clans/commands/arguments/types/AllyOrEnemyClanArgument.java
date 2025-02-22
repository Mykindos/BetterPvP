package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.function.Predicate;

/**
 * Prompts the sender with a list of allied and enemy Clans to the executor, guarantees a valid Clan return, but not a valid ally
 */
@Singleton
public class AllyOrEnemyClanArgument extends ClanArgument {
    @Inject
    protected AllyOrEnemyClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Ally or Enemy Clan";
    }

    protected Predicate<Clan> executorClanPredicate(Clan executorClan) {
        return clan -> clan.isAllied(executorClan) || clan.isEnemy(executorClan);
    }

}
