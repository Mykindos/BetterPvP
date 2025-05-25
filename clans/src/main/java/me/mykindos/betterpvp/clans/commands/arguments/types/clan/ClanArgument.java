package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Prompts the sender with a list of Clans, guarantees a valid Clan return
 */
@Singleton
public class ClanArgument extends BPvPArgumentType<Clan, String> implements CustomArgumentType.Converted<Clan, String> {
    protected final ClanManager clanManager;
    @Inject
    protected ClanArgument(ClanManager clanManager) {
        super("Clan");
        this.clanManager = clanManager;
    }
    /**
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public @NotNull Clan convert(@NotNull String nativeType) throws CommandSyntaxException {
        return clanManager.getClanByName(nativeType).orElseThrow(() -> ClanArgumentException.UNKNOWN_CLAN_NAME_EXCEPTION.create(nativeType));
    }

    /**
     * Converts the value from the native type to the custom argument type.
     * <p>
     * This method provides the command source for additional context when converting. You
     * may have to do your own {@code instanceof} checks for {@link CommandSourceStack}.
     *
     * @param nativeType native argument provided value
     * @param source     source of the command
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public <S> @NotNull Clan convert(@NotNull String nativeType, @NotNull S source) throws CommandSyntaxException {
        final Clan target = clanManager.getClanByName(nativeType).orElseThrow(() -> ClanArgumentException.UNKNOWN_CLAN_NAME_EXCEPTION.create(nativeType));
        if (!(source instanceof final CommandSourceStack sourceStack)) return target;

        final @Nullable Player executor = Bukkit.getPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());
        if (executor == null) return target;

        executorClanChecker(executor, target);

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
        return StringArgumentType.string();
    }

    /**
     * Provides a list of suggestions to show to the client.
     *
     * @param context command context
     * @param builder suggestion builder
     * @return suggestions
     */
    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof final CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);

        final @Nullable Player executor = Bukkit.getPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());
        clanManager.getObjects().values().stream()
                .filter(clan -> {
                    if (executor == null) return true;
                    try {
                        executorClanChecker(executor, clan);
                        return true;
                    } catch (CommandSyntaxException ignored) {
                        return false;
                    }
                })
                .map(Clan::getName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * With the given {@link Clan}, check if the given {@link Clan} can be matched against
     * should throw a {@link CommandSyntaxException} if invalid
     * @param executor the {@link Player} that is the executor
     * @param target the {@link Clan} that is being checked
     * @throws CommandSyntaxException if target is invalid
     */
    protected void executorClanChecker(@NotNull final Player executor, @NotNull final Clan target) throws CommandSyntaxException {
        //Intentionally left empty
    }

    @Nullable
    protected Clan getClanByExecutor(@Nullable Player executor) {
        return clanManager.getClanByPlayer(executor).orElse(null);
    }

}
