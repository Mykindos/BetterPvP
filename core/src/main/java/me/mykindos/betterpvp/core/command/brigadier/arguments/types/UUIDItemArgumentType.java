package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UUIDItemArgumentType extends BPvPArgumentType<UUIDItem, UUID> implements CustomArgumentType.Converted<UUIDItem, UUID> {

    private final UUIDManager uuidManager;
    @Inject
    protected UUIDItemArgumentType(UUIDManager uuidManager) {
        super("UUIDItem");
        this.uuidManager = uuidManager;
    }

    /**
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     */
    @Override
    public @NotNull UUIDItem convert(@NotNull UUID nativeType) throws CommandSyntaxException {
        return uuidManager.getObject(nativeType).orElseThrow(() -> ArgumentException.UNKNOWN_UUIDITEM.create(nativeType));
    }

    /**
     * Gets the native type that this argument uses,
     * the type that is sent to the client.
     *
     * @return native argument type
     */
    @Override
    public @NotNull ArgumentType<UUID> getNativeType() {
        return ArgumentTypes.uuid();
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
        uuidManager.getObjects().keySet().stream()
                .filter(uuid -> uuid.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
