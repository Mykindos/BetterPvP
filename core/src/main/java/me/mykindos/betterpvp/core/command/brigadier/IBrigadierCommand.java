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
import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Level;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IBrigadierCommand {
    //That this isn't default behavior does not make sense
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
    static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(final String name, final ArgumentType<T> argumentType, Predicate<CommandSourceStack> requirement) {
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
    static LiteralArgumentBuilder<CommandSourceStack> literal(final String literal) {
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
    static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(final String name, final ArgumentType<T> argumentType) {
        return Commands.argument(name, argumentType);
    }

    /**
     * Copies the literal, for command aliasing. Uses the same code as Paper's aliasing, but returning a
     * {@link LiteralArgumentBuilder} of {@link CommandSourceStack}
     * @param alias the alias name for the command
     * @param source the argument builder source
     * @return the copied Literal at {@code alias} of {@code source}
     * @see PaperBrigadier#copyLiteral(String, LiteralCommandNode)
     */
    @NotNull
    static LiteralArgumentBuilder<CommandSourceStack> copyLiteral(@NotNull String alias, @NotNull LiteralArgumentBuilder<CommandSourceStack> source) {
        LiteralArgumentBuilder<CommandSourceStack> copyBuilder = IBrigadierCommand.literal(alias)
                .requires(source.getRequirement())
                .forward(source.getRedirect(), source.getRedirectModifier(), source.isFork());
        if (source.getCommand() != null) {
            copyBuilder.executes(source.getCommand());
        }

        for(CommandNode<CommandSourceStack> child : source.build().getChildren()) {
            copyBuilder.then(child);
        }

        return copyBuilder;
    }

    /**
     * Used in retrieving path for config options
     * @return the name of this command
     */
    String getName();

    /**\
     * Builds the command to be registered
     * @return The command
     */
    LiteralCommandNode<CommandSourceStack> build();

    /**
     * Gets the description of this command, used in registration
     * @return the description
     */
    String getDescription();

    /**
     * Gets the aliases for this command, used in registration
     * @return the description
     */
    Collection<String> getAliases();

    /**
     * Sets the config for this command. Must be done before calling Build
     * @param config the config
     */
    void setConfig(ExtendedYamlConfiguration config);

    public Rank getRequiredRank();

    /**
     * Sets the parent of this command, used in finding the path
     * @param parent the parent command
     */
    void setParent(IBrigadierCommand parent);
    @Nullable IBrigadierCommand getParent();

    /**
     * Gets the Collection of children
     * @return the collection of children
     */
    Collection<IBrigadierCommand> getChildren();
    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     * @return the builder to be used in Build
     */
    LiteralArgumentBuilder<CommandSourceStack> define();

    /**
     * Get the requirement for running the command. Applied on build, even if this is a sub command
     * @param source the source
     * @return true if able to run, false otherwise
     */
    boolean requirement(CommandSourceStack source);


    /**
     * Outline the requirements to run this command and compare it to the current context
     * @param context the {@link CommandContext} to compare
     * @return the {@link Component} describing this command's requirements
     */
    Component getRequirementComponent(CommandContext<CommandSourceStack> context);

    /**
     * Get all the usages of this command
     * @param source the {@link CommandSourceStack}
     * @param parentUsage the usage of the parent to this command
     * @return the formatted string of all usages
     */
    String getUsages(CommandSourceStack source, @Nullable String parentUsage);

}
