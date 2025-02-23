package me.mykindos.betterpvp.clans.commands.arguments.types.clan;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.Clan2CommandExceptionType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Prompts the sender with a list of Clans, guarantees a valid Clan return
 */
@Singleton
public class ClanArgument extends BPvPArgumentType<Clan, String> implements CustomArgumentType.Converted<Clan, String> {
    public static final DynamicCommandExceptionType UNKNOWN_CLAN_NAME_EXCEPTION = new DynamicCommandExceptionType(
            (name) -> new LiteralMessage("Unknown Clan name " + name)
    );
    public static final DynamicCommandExceptionType NOT_IN_A_CLAN_EXCEPTION = new DynamicCommandExceptionType(
            (player) -> new LiteralMessage(player + " is not in a Clan")
    );
    public static final SimpleCommandExceptionType MUST_BE_IN_A_CLAN_EXCEPTION = new SimpleCommandExceptionType(
            new LiteralMessage("You must be in a Clan to use this command")
    );
    public static final Clan2CommandExceptionType CLAN_MUST_NOT_BE_SAME = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(origin.getName() + " must be a different Clan than " + target.getName())
    );
    //Ally
    public static final Clan2CommandExceptionType CLAN_NOT_NEUTRAL_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not Neutral to " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ENEMY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Enemy of " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ALLY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Ally of " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Ally or Enemy of " + origin.getName()));
    public static final Dynamic2CommandExceptionType CLAN_AT_MAX_SQUAD_COUNT_ALLY = new Dynamic2CommandExceptionType(
            (origin, size) -> new LiteralMessage(origin + " is at the maximum squad size " + size + " and cannot ally")
    );
    public static final Dynamic2CommandExceptionType CLAN_OVER_MAX_SQUAD_COUNT_ALLY = new Dynamic2CommandExceptionType(
            (clanName, size) -> new LiteralMessage(clanName + " has too high a squad count " + size + " to ally")
    );
    public static final Clan2CommandExceptionType CLAN_ALREADY_TRUSTS_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is already trusted by " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_DOES_NOT_TRUST_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " does not trust " + origin.getName())
    );




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
        return clanManager.getClanByName(nativeType).orElseThrow(() -> UNKNOWN_CLAN_NAME_EXCEPTION.create(nativeType));
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
        final Clan target = clanManager.getClanByName(nativeType).orElseThrow(() -> UNKNOWN_CLAN_NAME_EXCEPTION.create(nativeType));
        if (!(source instanceof final CommandSourceStack sourceStack)) return target;

        final Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());
        if (executorClanOptional.isEmpty()) return target;
        final Clan executorClan = executorClanOptional.get();
        executorClanChecker(executorClan, target);

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

    /**
     * Provides a list of suggestions to show to the client.
     *
     * @param context command context
     * @param builder suggestion builder
     * @return suggestions
     */
    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        //TODO handle non executor Clan (show all?) (maybe make it a config)
        if (!(context.getSource() instanceof final CommandSourceStack sourceStack)) return super.listSuggestions(context, builder);
        final Optional<Clan> executorClanOptional = clanManager.getClanByPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());
        if (executorClanOptional.isEmpty()) return super.listSuggestions(context, builder);
        final Clan executorClan = executorClanOptional.get();

        clanManager.getObjects().values().stream()
                .filter(clan -> {
                    try {
                        executorClanChecker(executorClan, clan);
                        return true;
                    } catch (CommandSyntaxException ignored) {
                        return false;
                    }
                })
                .map(Clan::getName)
                .filter(name -> name.contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * With the given {@link Clan}, check if the given {@link Clan} can be matched against
     * Should throw a {@link CommandSyntaxException} if invalid
     * @param executorClan the {@link Clan} that the executor is in
     * @param target the {@link Clan} that is being checked
     * @throws CommandSyntaxException if target is invalid
     */
    protected void executorClanChecker(Clan executorClan, Clan target) throws CommandSyntaxException {
    }

}
