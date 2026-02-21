package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.OnlinePlayerNameArgument;
import me.mykindos.betterpvp.core.effects.EffectManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents an invitable player name
 */
@Singleton
public class InvitablePlayerNameArgument extends OnlinePlayerNameArgument {


    private final ClanManager clanManager;

    @Inject
    public InvitablePlayerNameArgument(EffectManager effectManager, ClanManager clanManager, ClientManager clientManager) {
        super(effectManager, clientManager);
        this.clanManager = clanManager;
    }

    @Override
    public String getName() {
        return "Invitable Player";
    }

    @Override
    protected void playerChecker(@Nullable Player executor, @NotNull Player target) throws CommandSyntaxException {
        if (executor == null) {
            final Optional<Clan> targetClan = clanManager.getClanByPlayer(target);
            if (targetClan.isPresent()) {
                throw ClanArgumentException.MUST_NOT_BE_IN_A_CLAN_EXCEPTION.create(target.getName());
            }
            return;
        }

        final Client executorClient = clientManager.search().online(executor);
        final Client targetClient = clientManager.search().online(target);
        clanManager.canInviteToClan(executorClient, targetClient);
    }
}
