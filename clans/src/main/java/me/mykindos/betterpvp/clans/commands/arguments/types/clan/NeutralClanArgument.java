package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import org.jetbrains.annotations.NotNull;

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

    /**
     * With the given {@link Clan}, check if the given {@link Clan} can be matched against
     * Should throw a {@link CommandSyntaxException} if invalid
     * @param executorClan the {@link Clan} that the executor is in
     * @param target the {@link Clan} that is being checked
     * @throws CommandSyntaxException if target is invalid
     */
    @Override
    protected void executorClanChecker(@NotNull Clan executorClan, @NotNull Clan target) throws CommandSyntaxException {
        if (executorClan.isAllied(target) || executorClan.isEnemy(target) || executorClan.equals(target)) {
            throw ClanArgument.CLAN_NOT_NEUTRAL_OF_CLAN.create(executorClan, target);
        }
    }
}