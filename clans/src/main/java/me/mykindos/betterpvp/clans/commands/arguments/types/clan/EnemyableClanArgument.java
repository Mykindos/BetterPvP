package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Prompts the sender with a list of enemyable Clans to the executor, guarantees a valid Clan return, but not a valid allyable clan
 */
@Singleton
public class EnemyableClanArgument extends ClanArgument {
    @Inject
    protected EnemyableClanArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Enemyable Clan";
    }

    @Override
    protected void executorClanChecker(@NotNull final Player executor, @NotNull final Clan target) throws CommandSyntaxException {
        final Clan executorClan = clanManager.getClanByPlayer(executor).orElse(null);
        if (executorClan == null) return;
        clanManager.canEnemyThrow(executorClan, target);
    }
}