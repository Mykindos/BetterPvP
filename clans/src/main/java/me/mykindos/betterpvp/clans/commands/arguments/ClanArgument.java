package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;

import java.util.concurrent.CompletableFuture;

@Singleton
public class ClanArgument extends BPvPArgumentType<Clan, String> implements CustomArgumentType.Converted<Clan, String> {

    private final ClanManager clanManager;
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
        return clanManager.getClanByName(nativeType).orElseThrow(() -> BPvPClansArgumentTypes.UNKOWNCLANNAMEEXCEPTION.create(nativeType));
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
