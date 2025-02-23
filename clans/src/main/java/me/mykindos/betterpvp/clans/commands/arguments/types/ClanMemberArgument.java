package me.mykindos.betterpvp.clans.commands.arguments.types;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ClanMemberArgument extends PlayerNameArgumentType {
    private final ClanManager clanManager;

    @Inject
    public ClanMemberArgument (ClanManager clanManager) {
        super();
        this.clanManager = clanManager;
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
    public <S> @NotNull String convert(String nativeType, S source) throws CommandSyntaxException {
        return super.convert(nativeType, source);
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
        return super.listSuggestions(context, builder);
    }
}
