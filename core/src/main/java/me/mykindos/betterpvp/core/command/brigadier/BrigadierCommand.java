package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    private final Collection<IBrigadierCommand> children = new ArrayList<>();

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
    public LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = define();
        this.children.forEach(child -> {
            log.info("Defining child: {}", child.getName()).submit();
            LiteralArgumentBuilder<CommandSourceStack> childArgument = child.define().requires(child::requirement);
            //define the child
            root.then(child.define().requires(child::requirement));

            //add all aliases like Paper does for the root
            for (String alias : child.getAliases()) {
                log.info("Adding alias: {}", alias).submit();
                root.then(IBrigadierCommand.copyLiteral(alias, childArgument));
            }
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
     * Checks if the {@link CommandSourceStack#getSender()} has the correct {@link Rank} to run this command
     * @param source the {@link CommandSourceStack}
     * @return {@code false} if {@link CommandSourceStack#getSender()} is a {@link Player}
     * but does not have the required {@link Rank},
     * {@code true} otherwise (i.e. console)
     */
    protected boolean senderHasCorrectRank(CommandSourceStack source) {
        if (source.getSender() instanceof final Player sender) {
            return clientManager.search().online(sender).hasRank(requiredRank);
        }
        //CommandSender is not a player, always allow
        return true;
    }

    /**
     * Checks if the {@link CommandSourceStack#getSender()} is {@link Client#isAdministrating() administrating}
     * @param source the {@link CommandSourceStack}
     * @return {@code true} if the {@link CommandSourceStack#getSender()} is a {@link Player} and is {@link Client#isAdministrating() administrating}
     */
    protected boolean senderIsAdministrating(CommandSourceStack source) {
        if (source.getSender() instanceof final Player sender) {
            return clientManager.search().online(sender).isAdministrating();
        }
        return false;
    }

    /**
     *
     * @param source the {@link CommandSourceStack}
     * @return {@link true} if the {@link CommandSourceStack#getSender()} has the permission {@code minecraft.command.selector}
     * @see CommandSender#hasPermission(String)
     */
    protected boolean senderHasSelector(CommandSourceStack source) {
        return source.getSender().hasPermission("minecraft.command.selector");
    }

    protected boolean commandIsEnabled() {
        return enabled;
    }

    /**
     * Gets the {@link Player} executing this command
     * @param context the {@link CommandContext}
     * @return the {@link Player} executing this command
     * @throws CommandSyntaxException if {@link CommandSourceStack#getExecutor()} not {@code instanceof} {@link Player}
     */
    @NotNull
    protected Player getPlayerFromExecutor(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getExecutor() instanceof final Player player)) throw ArgumentException.TARGET_MUST_BE_PLAYER.create(context.getSource().getExecutor() == null ? null : (context.getSource().getExecutor().getName()));
        return player;
    }

    /**
     * Gets the {@link Client} of the {@link CommandSourceStack#getExecutor()}
     * @param context the {@link CommandContext<CommandSourceStack>}
     * @return the {@link Client}
     * @throws ClassCastException if {@link CommandSourceStack#getExecutor()} is not instance of {@link Player}
     */
    @NotNull
    protected Client getClientFromExecutor(@NotNull CommandContext<CommandSourceStack> context) {
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
                                .sendMessage(UtilMessage.deserialize("<red>" + PlayerNameArgumentType.UNKNOWN_PLAYER_EXCEPTION
                                        .create(name).getMessage()));
                    }
                    return clientOptional;
                }).exceptionally(throwable -> {
                    log.error("Error retrieving offline player for command {}", getName(), throwable).submit();
                    return Optional.empty();
        });
    }
}
