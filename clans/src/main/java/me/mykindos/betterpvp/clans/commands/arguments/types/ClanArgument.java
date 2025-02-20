package me.mykindos.betterpvp.clans.commands.arguments.types;

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
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;

import java.util.concurrent.CompletableFuture;

/**
 * Prompts the sender with a list of Clans, guarantees a valid Clan return
 */
@Singleton
public class ClanArgument extends BPvPArgumentType<Clan, String> implements CustomArgumentType.Converted<Clan, String> {
    public static DynamicCommandExceptionType UNKOWN_CLAN_NAME_EXCEPTION = new DynamicCommandExceptionType((name) -> new LiteralMessage("Unknown Clan name " + name));
    public static DynamicCommandExceptionType NOT_IN_A_CLAN_EXCEPTION = new DynamicCommandExceptionType((player) -> new LiteralMessage(player + " is not in a Clan"));
    public static SimpleCommandExceptionType MUST_BE_IN_A_CLAN_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("You must be in a Clan to use this command"));
    public static Dynamic2CommandExceptionType CLAN_NOT_ENEMY_OF_CLAN = new Dynamic2CommandExceptionType((origin, target) -> new LiteralMessage(target + " is not an Enemy of " + origin));
    public static Dynamic2CommandExceptionType CLAN_NOT_ALLY_OF_CLAN = new Dynamic2CommandExceptionType((origin, target) -> new LiteralMessage(target + " is not an Ally of " + origin));
    public static Dynamic2CommandExceptionType CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN = new Dynamic2CommandExceptionType((origin, target) -> new LiteralMessage(target + " is not an Ally or Enemy of " + origin));

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
    public Clan convert(String nativeType) throws CommandSyntaxException {
        return clanManager.getClanByName(nativeType).orElseThrow(() -> UNKOWN_CLAN_NAME_EXCEPTION.create(nativeType));
    }

    /**
     * Gets the native type that this argument uses,
     * the type that is sent to the client.
     *
     * @return native argument type
     */
    @Override
    public ArgumentType<String> getNativeType() {
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
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        clanManager.getObjects().values().stream()
                .map(Clan::getName)
                .filter(name -> name.contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

}
