package me.mykindos.betterpvp.core.command.brigadier.arguments.types;

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
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Singleton
public class OnlinePlayerNameArgument extends BPvPArgumentType<Player, String> implements CustomArgumentType.Converted<Player, String> {
    //TODO refine can see check to admin vanish
    private final EffectManager effectManager;
    protected final ClientManager clientManager;
    @Inject
    public OnlinePlayerNameArgument(EffectManager effectManager, ClientManager clientManager) {
        super("Online Player");
        this.effectManager = effectManager;
        this.clientManager = clientManager;
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
     * Converts the value from the native type to the custom argument type.
     *
     * @param nativeType native argument provided value
     * @return converted value
     * @throws CommandSyntaxException if an exception occurs while parsing
     * @see #convert(Object, Object)
     */
    @Override
    public @NotNull Player convert(@NotNull String nativeType) throws CommandSyntaxException {
        final Player player = Bukkit.getPlayerExact(nativeType);
        if (player == null) {
            throw ArgumentException.UNKNOWN_PLAYER.create(nativeType);
        }

        playerChecker(null, player);

        return player;
    }

    @Override
    public <S> @NotNull Player convert(@NotNull String nativeType, @NotNull S source) throws CommandSyntaxException {
        final Player player = Bukkit.getPlayerExact(nativeType);
        if (player == null) {
            throw ArgumentException.UNKNOWN_PLAYER.create(nativeType);
        }

        if (!(source instanceof CommandSourceStack sourceStack)) return player;

        final @Nullable Player executor = Bukkit.getPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());

        if (executor != null &&
                clientManager.search().online(executor).hasRank(Rank.HELPER)
                && effectManager.hasEffect(player, EffectTypes.VANISH, "commandVanish"))
        {
            throw ArgumentException.UNKNOWN_PLAYER.create(nativeType);
        }
        playerChecker(executor, player);

        return player;
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof final CommandSourceStack sourceStack))
            return super.listSuggestions(context, builder);

        final @Nullable Player executor = Bukkit.getPlayer(Objects.requireNonNull(sourceStack.getExecutor()).getUniqueId());

        Bukkit.getOnlinePlayers().stream()
                .filter(target -> executor == null ||
                        !clientManager.search().online(executor).hasRank(Rank.HELPER) ||
                        !effectManager.hasEffect(target, EffectTypes.VANISH, "commandVanish"))
                .filter(player -> {
                    try {
                        playerChecker(executor, player);
                    } catch (CommandSyntaxException ignored) {
                        return false;
                    }

                    if (!(sourceStack.getSender() instanceof Player sender)) return true;
                    return sender.canSee(player);
                })
                .map(Player::getName)
                .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Check to see if the given {@link Player}
     * should throw a {@link CommandSyntaxException} if invalid
     * @param target the {@link Player} being checked
     * @throws CommandSyntaxException if target is invalid
     */
    protected void playerChecker(@Nullable Player executor, @NotNull Player target) throws CommandSyntaxException {
        //intentionally empty, extended classes should override
    }
}
