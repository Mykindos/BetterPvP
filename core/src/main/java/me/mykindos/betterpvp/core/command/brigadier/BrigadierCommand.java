package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public abstract class BrigadierCommand implements IBrigadierCommand {

    private final ClientManager clientManager;


    @Setter
    private Rank requiredRank;
    @Setter
    private boolean enabled;
    @Getter
    private final Set<String> aliases = new HashSet<>();

    protected BrigadierCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     * @return the builder to be used in Build
     */
    protected abstract LiteralArgumentBuilder<CommandSourceStack> define();


    /**
     * Builds the LiteralCommandNode<CommandSourceStack> from define()
     * Requires sender to have required rank and executor to be a player
     * @return the build
     */
    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return define().requires(this::requirement).build();
    }


    /**
     * Defines the requirements this command needs to be runnable
     * Default: Command is Enabled, Executor is a player, Sender has correct rank
     * <p>Used in BrigadierCommand#build()</p>
     * @param source the CommandSourceStack
     * @return whether the runner can use the command
     */
    protected boolean requirement(CommandSourceStack source) {
            return enabled && executorIsPlayer(source) && senderHasCorrectRank(source);
    }

    //Helper Methods

    /**
     * checks if the source Executor is a player
     * @param source the CommandSourceStack
     * @return true if CommandSourceStack#getExecutor instance of Player, false otherwise
     */
    protected static boolean executorIsPlayer(CommandSourceStack source) {
        return (source.getExecutor() instanceof Player);
    }

    /**
     * Checks if the sender has the correct permission to run this command
     * @param source the CommandSourceStack
     * @return false if CommandSourceStack#getSender() instance of player
     * but does not have the required rank,
     * true otherwise (i.e. console)
     */
    protected boolean senderHasCorrectRank(CommandSourceStack source) {
        if (source.getSender() instanceof Player sender) {
            return clientManager.search().online(sender).hasRank(requiredRank);
        }
        //CommandSender is not a player, always allow
        return true;
    }

    /**
     * Gets the client of the executor
     * @param context the CommandContext
     * @return the Client
     * @throws ClassCastException if CommandContext<CommandSourceStack>#getSource()#getExectutor() is not instance of Player
     */
    protected Client getClientFromExecutor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            throw new ClassCastException("Cannot get a client of a non-player");
        }
        return clientManager.search().online(player);
    }
}
