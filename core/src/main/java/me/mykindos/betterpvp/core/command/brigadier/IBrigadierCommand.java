package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IBrigadierCommand {
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

}
