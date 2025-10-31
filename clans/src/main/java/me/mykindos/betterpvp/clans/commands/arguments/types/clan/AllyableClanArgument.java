package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    @Override
    protected void executorClanChecker(@NotNull Player executor, @NotNull Clan target) throws CommandSyntaxException {
        final Clan executorClan = getClanByExecutor(executor);
        if (executorClan == null) return;
        clanManager.canAllyThrow(executorClan, target);
    }
}