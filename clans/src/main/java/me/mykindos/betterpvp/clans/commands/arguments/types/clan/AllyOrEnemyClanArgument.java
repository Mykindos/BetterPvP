package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Override
    protected void executorClanChecker(@Nullable Player executor, @NotNull Clan target) throws CommandSyntaxException {
        final Clan executorClan = getClanByExecutor(executor);
        if (executorClan == null) return;
        if (!(executorClan.isAllied(target) || executorClan.isEnemy(target))) {
            throw ClanArgumentException.CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN.create(executorClan, target);
        }
    }
}
