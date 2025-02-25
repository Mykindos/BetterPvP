package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Shows a prompt message that this is a ClanName. Enforces that the returned name contains valid characters and is correct length
 */
@Singleton
public class ClanNameArgument extends BPvPArgumentType<String, String> implements CustomArgumentType.Converted<String, String> {
    public final static Dynamic3CommandExceptionType INVALID_CLAN_NAME = new Dynamic3CommandExceptionType((name, min, max) ->
        new LiteralMessage(name + " is not a valid Clan name. Clan names must be between " + min + " and " + max + " characters long and only include alphanumeric characters and '_'")
    );

    public final static DynamicCommandExceptionType NAME_ALREADY_EXISTS = new DynamicCommandExceptionType((name) ->
            new LiteralMessage(name + "is already in use by another Clan")
    );

    public final static DynamicCommandExceptionType NAME_IS_FILTERED = new DynamicCommandExceptionType((name) ->
            new LiteralMessage(name + " contains a filtered word")
    );
    private final ClanManager clanManager;

    @Inject
    public ClanNameArgument(ClanManager clanManager) {
        super("Clan Name");
        this.clanManager = clanManager;
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
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        //TODO would throwing something here work to show?
        builder.suggest("", new LiteralMessage("Clan Name"));
        return builder.buildFuture();
    }

    //TODO change to other type, and return completable future that messages sender if fileter fails
    /**
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public @NotNull String convert(String nativeType) throws CommandSyntaxException {
        if (!nativeType.matches("^[a-zA-Z0-9_]{" + clanManager.getMinCharactersInClanName() + "," + clanManager.getMaxCharactersInClanName() + "}$")) {
            throw INVALID_CLAN_NAME.create(nativeType, clanManager.getMinCharactersInClanName(), clanManager.getMaxCharactersInClanName());
        }
        if (clanManager.getClanByName(nativeType).isPresent()) {
            throw NAME_ALREADY_EXISTS.create(nativeType);
        }
        return nativeType;
    }
}
