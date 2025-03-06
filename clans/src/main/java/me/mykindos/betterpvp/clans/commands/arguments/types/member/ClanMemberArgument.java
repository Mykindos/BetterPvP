package me.mykindos.betterpvp.clans.commands.arguments.types.member;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ClanMemberArgument extends BPvPArgumentType<ClanMember, String> implements CustomArgumentType.Converted<ClanMember, String> {

    protected final ClanManager clanManager;

    @Inject
    public ClanMemberArgument(ClanManager clanManager) {
        super("Clan Member");
        this.clanManager = clanManager;
    }

    //TODO convert to return the actual ClanMember
    @Override
    public <S> @NotNull ClanMember convert(@NotNull String nativeType, @NotNull S source) throws CommandSyntaxException {
        final String name = nativeType;

        if (!(source instanceof final CommandSourceStack sourceStack)) throw new ClassCastException("source type of invalid type");
        final @NotNull Entity executor = Objects.requireNonNull(sourceStack.getExecutor());

        final Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(executor.getUniqueId());
        if (executorClanOptional.isEmpty()) throw ClanArgumentException.MUST_BE_IN_A_CLAN_EXCEPTION.create();

        final Clan executorClan = executorClanOptional.get();
        final ClanMember executorMember = executorClan.getMember(executor.getUniqueId());
        final ClanMember target = executorClan.getMemberByName(name).orElseThrow(() -> ClanArgumentException.MEMBER_NOT_MEMBER_OF_CLAN.create(executorClan.getName(), name));

        clanMemberChecker(executorMember, target);
        return target;
    }

    /**
     * Gets the native type that this argument uses,
     * the type that is sent to the client.
     *
     * @return native argument type
     */
    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
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
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     * @see #convert(Object, Object)
     */
    @Override
    public @NotNull ClanMember convert(@NotNull String nativeType) throws CommandSyntaxException {
        /// as of 3/4/25 Paper first calls {@link CustomArgumentType#parse(StringReader, Object)} which is implemented in
        /// {@link CustomArgumentType.Converted} by calling {@link CustomArgumentType.Converted#convert(Object, Object)}.
        /// as long as {@link CustomArgumentType.Converted#convert(Object, Object)} is overriden, we do not need this method,
        /// as the default implementation of {@link CustomArgumentType.Converted#convert(Object)} just calls {@link CustomArgumentType.Converted#convert(Object, Object)}

        //this method should never be called since 1.21.4 #184
        throw new UnsupportedOperationException("Convert#Object is not implemented");
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
