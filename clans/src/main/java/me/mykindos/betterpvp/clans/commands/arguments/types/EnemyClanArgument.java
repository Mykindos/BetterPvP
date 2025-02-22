package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.function.Predicate;

/**
 * Prompts the sender with a list of enemy Clans to the executor, guarantees a valid Clan return, but not a valid enemy
 */
@Singleton
public class EnemyClanArgument extends ClanArgument {
    @Inject
    protected EnemyClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Enemy Clan";
    }

    @Override
    protected Predicate<Clan> executorClanPredicate(Clan executorClan) {
        return executorClan::isEnemy;
    }
}
