package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Prompts the sender with a list of joinable Clans to the executor, guarantees a valid Clan return, but not a valid Joinable clan
 */
@Singleton
public class JoinableClanArgument extends ClanArgument {
    private final ClientManager clientManager;
    @Inject
    protected JoinableClanArgument(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager);
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "Joinable Clan";
    }

    @Override
    protected void executorClanChecker(@NotNull Player executor, @NotNull Clan target) throws CommandSyntaxException {
        final Client executorClient = clientManager.search().online(executor);
        clanManager.canJoinClan(executorClient, target);
    }
}