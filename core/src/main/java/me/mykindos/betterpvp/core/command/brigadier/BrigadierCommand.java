package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@CustomLog
public abstract class BrigadierCommand implements IBrigadierCommand {
    protected final ClientManager clientManager;

    @Getter
    @Setter
    private IBrigadierCommand parent;
    @Getter
    private Collection<IBrigadierCommand> children = new ArrayList<>();

    protected ExtendedYamlConfiguration config;
    @Getter
    private final Set<String> aliases = new HashSet<>();

    private boolean enabled;
    private Rank requiredRank;

    protected BrigadierCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void setConfig(ExtendedYamlConfiguration config) {
        //todo do this better (loads before it should)
        this.config = config;
        String rankPath = getPath() + ".requiredRank";
        this.requiredRank = Rank.valueOf(config.getOrSaveString(rankPath, "ADMIN").toUpperCase());
        this.enabled = config.getOrSaveBoolean(getPath() + ".enabled", true);
        this.children.forEach(child -> child.setConfig(config));
    }

    public void loadConfig() {

    }

    public String getPath() {

        StringBuilder path = new StringBuilder(getName());
        IBrigadierCommand currentParent = getParent();
        while (currentParent != null) {
            path.insert(0, currentParent.getName() + ".");
            currentParent = currentParent.getParent();
        }
        String pathString = path.toString();
        log.info(pathString).submit();
        return pathString;
    }


    /**
     * Builds the LiteralCommandNode<CommandSourceStack> from define()
     * Requires sender to have required rank and executor to be a player
     * @return the build
     */
    @Override
    //TODO might have to do muliple builds for subcommand aliases
    public LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = define();
        this.children.forEach(child -> {
            log.info("Defining child: {}", child.getName()).submit();
            root.then(child.define().requires(child::requirement));
        });
        return root.requires(this::requirement).build();
    }


    /**
     * Defines the requirements the root command needs to be runnable
     * Default: Command is Enabled, Executor is a player, Sender has correct rank
     * <p>Used in BrigadierCommand#build()</p>
     * @param source the CommandSourceStack
     * @return whether the runner can use the command
     */
    @Override
    public boolean requirement(CommandSourceStack source) {
            return commandIsEnabled() && executorIsPlayer(source) && senderHasCorrectRank(source);
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

    protected boolean commandIsEnabled() {
        return enabled;
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

    //Since we cannot throw a CommandSyntaxException in async contexts, this will pseudo throw on an empty optional.
    
    /**
     * Gets the requested client by name, or informs the CommandSender that it does not exist
     * If optional is empty, inform player
     * @param name the name of the client
     * @param commandSender the player sending the command
     */
    protected CompletableFuture<Optional<Client>> getOfflineClientByName(String name, CommandSender commandSender) {
        return clientManager.search().offline(name).thenApply(clientOptional -> {
                    if (clientOptional.isEmpty()) {
                        commandSender
                                .sendMessage(UtilMessage.deserialize("<red>" + PlayerNameArgumentType.UNKNOWNPLAYEREXCEPTION
                                        .create(name).getMessage()));
                    }
                    return clientOptional;
                }).exceptionally(throwable -> {
                    log.error("Error retrieving offline player for command {}", getName(), throwable).submit();
                    return Optional.empty();
        });
    }
}
