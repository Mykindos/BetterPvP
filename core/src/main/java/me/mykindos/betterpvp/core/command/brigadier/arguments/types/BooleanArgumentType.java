package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Singleton
public class BooleanArgumentType extends BPvPArgumentType<Boolean, String> implements CustomArgumentType.Converted<Boolean, String> {
    @Inject
    protected BooleanArgumentType() {
        super("Boolean");
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
    public @NotNull Boolean convert(@NotNull String nativeType) throws CommandSyntaxException {
        return Boolean.parseBoolean(nativeType);
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
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, SuggestionsBuilder builder) {
        if (Boolean.TRUE.toString().toLowerCase().contains(builder.getRemainingLowerCase())) {
            builder.suggest(Boolean.TRUE.toString());
        }
        if (Boolean.FALSE.toString().toLowerCase().contains(builder.getRemainingLowerCase())) {
            builder.suggest(Boolean.FALSE.toString());
        }
        return builder.buildFuture();
    }
}
