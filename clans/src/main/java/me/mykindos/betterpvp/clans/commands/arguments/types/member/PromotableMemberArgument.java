package me.mykindos.betterpvp.clans.commands.arguments.types.member;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.jetbrains.annotations.NotNull;


@Singleton
@CustomLog
public class PromotableMemberArgument extends ClanMemberArgument {

    @Inject
    public PromotableMemberArgument(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "Promotable Clan Member";
    }

    /**
     * With the given {@link ClanMember}, check if the given {@link ClanMember} can be matched against
     * should throw a {@link CommandSyntaxException} if invalid
     * @param executor the {@link ClanMember} of the {@link CommandSourceStack#getExecutor() executor}
     * @param target the {@link ClanMember} being checked
     * @throws CommandSyntaxException if target is invalid
     */
    @Override
    protected void clanMemberChecker(@NotNull ClanMember executor, @NotNull ClanMember target) throws CommandSyntaxException {
        log.info("Checking {} against target {}", executor.getClientName(), target.getClientName()).submit();
        clanManager.canPromoteThrow(executor, target);
    }
}
