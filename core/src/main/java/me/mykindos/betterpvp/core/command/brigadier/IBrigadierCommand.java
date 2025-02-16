package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.core.client.Rank;

import java.util.Collection;

public interface IBrigadierCommand {
    String getName();
    void setEnabled(boolean enabled);
    void setRequiredRank(Rank requiredRank);

    /**\
     * Builds the command to be registered
     * @return The command
     */
    LiteralCommandNode<CommandSourceStack> build();

    String getDescription();

    Collection<String> getAliases();


    //todo description + aliases

}
