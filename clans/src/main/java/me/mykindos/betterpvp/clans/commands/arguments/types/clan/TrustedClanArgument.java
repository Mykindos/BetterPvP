package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;

/**
 * Prompts the sender with a list of allied Clans to the executor, guarantees a valid Clan return, but not a valid ally
 */
@Singleton
public class TrustedClanArgument extends ClanArgument {
    @Inject
    protected TrustedClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Trusted Clan";
    }

    /**
     * With the given {@link Clan}, check if the given {@link Clan} can be matched against
     * Should throw a {@link CommandSyntaxException} if invalid
     * @param executorClan the {@link Clan} that the executor is in
     * @param target the {@link Clan} that is being checked
     * @throws CommandSyntaxException if target is invalid
     */
    @Override
    protected void executorClanChecker(Clan executorClan, Clan target) throws CommandSyntaxException {
        if (!executorClan.hasTrust(target)) {
            throw ClanArgument.CLAN_DOES_NOT_TRUST_CLAN.create(executorClan, target);
        }
    }

}
