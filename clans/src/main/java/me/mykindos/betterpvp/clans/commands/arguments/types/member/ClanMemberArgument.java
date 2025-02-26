package me.mykindos.betterpvp.clans.commands.arguments.types.member;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ClanMemberArgument extends PlayerNameArgumentType {

    public static final SimpleCommandExceptionType MEMBER_CLAN_CANNOT_ACTION_SELF = new SimpleCommandExceptionType(
            new LiteralMessage("You cannot do this action to yourself")
    );

    public static final DynamicCommandExceptionType MEMBER_CANNOT_ACTION_MEMBER_RANK = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage("Rank insufficient to do this action to " + targetName)
    );

    public static final Dynamic2CommandExceptionType MEMBER_NOT_MEMBER_OF_CLAN = new Dynamic2CommandExceptionType(
            (clanName, targetName) -> new LiteralMessage(targetName + "is not a member of " + clanName)
    );

    public static final DynamicCommandExceptionType TARGET_MEMBER_RANK_TOO_LOW = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage(targetName + "'s rank is too low for this action to be performed")
    );
    public static final DynamicCommandExceptionType TARGET_MEMBER_IN_ENEMY_TERRITORY = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage(targetName + " cannot be kicked in enemy territory")
    );

    protected final ClanManager clanManager;

    @Inject
    public ClanMemberArgument(ClanManager clanManager) {
        super();
        this.clanManager = clanManager;
    }

    @Override
    public <S> @NotNull String convert(@NotNull String nativeType, @NotNull S source) throws CommandSyntaxException {
        final String name = super.convert(nativeType, source);
        //todo executor logic
        if (!(source instanceof final CommandSourceStack sourceStack)) return name;
        final @NotNull Entity executor = Objects.requireNonNull(sourceStack.getExecutor());

        final Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(executor.getUniqueId());
        if (executorClanOptional.isEmpty()) return name;

        final Clan executorClan = executorClanOptional.get();
        final ClanMember executorMember = executorClan.getMember(executor.getUniqueId());
        final ClanMember target = executorClan.getMemberByName(name).orElseThrow(() -> MEMBER_NOT_MEMBER_OF_CLAN.create(executorClan.getName(), name));

        clanMemberChecker(executorMember, target);
        return name;
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof final CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);
        final @NotNull Entity executor = Objects.requireNonNull(sourceStack.getExecutor());
        final Clan executorClan = clanManager.getClanByPlayer(executor.getUniqueId()).orElse(null);
        if (executorClan == null) {
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.equalsIgnoreCase(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        }

        final ClanMember executorMember = executorClan.getMember(executor.getUniqueId());

        executorClan.getMembers().stream()
                .filter(member -> {
                    try {
                        clanMemberChecker(executorMember, member);
                        return true;
                    } catch (CommandSyntaxException ignored) {
                        return false;
                    }
                })
                .map(ClanMember::getClientName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    /**
     * With the given {@link ClanMember}, check if the given {@link ClanMember} can be matched against
     * should throw a {@link CommandSyntaxException} if invalid
     * @param executor the {@link ClanMember} of the {@link CommandSourceStack#getExecutor() executor}
     * @param target the {@link ClanMember} being checked
     * @throws CommandSyntaxException if target is invalid
     */
    protected void clanMemberChecker(@NotNull ClanMember executor, @NotNull ClanMember target) throws CommandSyntaxException {
        //intentionally blank, not throwing is true
    }
}
