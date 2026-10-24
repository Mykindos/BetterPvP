package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.PaperBrigadier;
import java.util.function.Predicate;
import java.util.logging.Level;
import me.mykindos.betterpvp.core.Core;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Builders for the nodes of a Brigadier command tree, typed to {@link CommandSourceStack}.
 */
public final class BrigadierNodes {

    private BrigadierNodes() {
    }

    /**
     * <P>Utility to create a required argument builder with the correct generic.</P>
     * Sets the {@link RequiredArgumentBuilder#requires(Predicate) requirement}
     * and only shows the {@link ArgumentType#listSuggestions(CommandContext, SuggestionsBuilder)}  suggestions} if the {@link RequiredArgumentBuilder#requires(Predicate) requirement} is met.
     * @apiNote do not use {@link RequiredArgumentBuilder#requires(Predicate)} or {@link RequiredArgumentBuilder#suggests(SuggestionProvider)}
     * or functionality will break
     * @param name         the name of the argument
     * @param argumentType the type of the argument
     * @param <T>          the generic type of the argument value
     * @return a new required argument builder
     * @see Commands#argument(String, ArgumentType)
     */
    @NotNull
    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(final String name, final ArgumentType<T> argumentType, Predicate<CommandSourceStack> requirement) {
        RequiredArgumentBuilder<CommandSourceStack, T> builder = Commands.argument(name, argumentType);
        builder.requires(requirement);
        builder.suggests((context, suggestionsBuilder) -> {
            if (requirement.test(context.getSource())) {
                return argumentType.listSuggestions(context, suggestionsBuilder).exceptionally(throwable -> {

                    JavaPlugin.getPlugin(Core.class).getLogger().log(Level.SEVERE, "Error generating required suggestion: ", throwable);
                    return Suggestions.empty().join();
                });
            }
            return Suggestions.empty();
        });
        return builder;
    }

    /**
     * Utility to create a literal command node builder with the correct generic.
     *
     * @param literal literal name
     * @return a new builder instance
     * @see Commands#literal(String)
     */
    @NotNull
    public static LiteralArgumentBuilder<CommandSourceStack> literal(final String literal) {
        return Commands.literal(literal);
    }

    /**
     * Utility to create a required argument builder with the correct generic.
     *
     * @param name         the name of the argument
     * @param argumentType the type of the argument
     * @param <T>          the generic type of the argument value
     * @return a new required argument builder
     * @see Commands#argument(String, ArgumentType)
     */
    @NotNull
    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(final String name, final ArgumentType<T> argumentType) {
        return Commands.argument(name, argumentType);
    }

    /**
     * Copies the literal, for command aliasing. Uses the same code as Paper's aliasing.
     * The copy shares the children of {@code source}, so every nested subcommand is reachable through the alias.
     * @param alias the alias name for the command
     * @param source the built command node
     * @return the copied Literal at {@code alias} of {@code source}
     * @see PaperBrigadier#copyLiteral(String, LiteralCommandNode)
     */
    @NotNull
    public static LiteralCommandNode<CommandSourceStack> copyLiteral(@NotNull String alias, @NotNull LiteralCommandNode<CommandSourceStack> source) {
        LiteralArgumentBuilder<CommandSourceStack> copyBuilder = literal(alias)
                .requires(source.getRequirement())
                .forward(source.getRedirect(), source.getRedirectModifier(), source.isFork());
        if (source.getCommand() != null) {
            copyBuilder.executes(source.getCommand());
        }

        for (CommandNode<CommandSourceStack> child : source.getChildren()) {
            copyBuilder.then(child);
        }

        return copyBuilder.build();
    }
}
