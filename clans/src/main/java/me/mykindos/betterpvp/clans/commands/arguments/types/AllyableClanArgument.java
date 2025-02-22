package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

import java.util.function.Predicate;

/**
 * Prompts the sender with a list of allyable Clans to the executor, guarantees a valid Clan return, but not a valid allyable clan
 */
@Singleton
public class AllyableClanArgument extends ClanArgument {
    @Inject
    protected AllyableClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Allyable Clan";
    }

    protected Predicate<Clan> executorClanPredicate(Clan executorClan) {
        return clan -> clanManager.canAlly(executorClan, clan);
    }
}