package me.mykindos.betterpvp.core.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.arguments.types.PlayerNameArgumentType;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CustomLog
public abstract class BrigadierCommand {
    protected final ClientManager clientManager;

    /**
     * The command this is a subcommand of, used in finding the config path. {@code null} for a top-level command.
     */
    @Getter
    @Setter
    @Nullable
    private BrigadierCommand parent;
    @Getter
    private final Collection<BrigadierCommand> children = new ArrayList<>();

    protected ExtendedYamlConfiguration config;
    @Getter
    private final Set<String> aliases = new HashSet<>();

    private boolean enabled;
    @Getter
    private Rank requiredRank;

    protected BrigadierCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Used in retrieving path for config options
     * @return the name of this command
     */
    public abstract String getName();

    /**
     * Gets the description of this command, used in registration
     * @return the description
     */
    public abstract String getDescription();

    /**
     * Gets the description as a component, which may be translatable
     * @return the description
     */
    public Component getDescriptionComponent() {
        return Component.text(getDescription());
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     * @return the builder to be used in Build
     */
    public abstract LiteralArgumentBuilder<CommandSourceStack> define();

    /**
     * Sets the config for this command and its children. Must be done before calling {@link #build()}
     * @param config the config
     */
    public void setConfig(ExtendedYamlConfiguration config) {
        this.config = config;
        String rankPath = getPath() + ".requiredRank";
        try {
            this.requiredRank = Rank.valueOf(config.getOrSaveString(rankPath, "ADMIN").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.requiredRank = Rank.ADMIN;
            log.error("Unknown Rank {} for command {} ", config.getString(rankPath), getPath(), e).submit();
        }
        this.enabled = config.getOrSaveBoolean(getPath() + ".enabled", true);
        this.children.forEach(child -> child.setConfig(config));
    }

    public void loadConfig() {

    }

    public String getPath() {

        StringBuilder path = new StringBuilder(getName());
        BrigadierCommand currentParent = getParent();
        while (currentParent != null) {
            path.insert(0, currentParent.getName() + ".");
            currentParent = currentParent.getParent();
        }
        return path.toString();
    }


    /**
     * Builds the LiteralCommandNode<CommandSourceStack> from define()
     * Requires the command to be enabled and the sender to have the required rank
     * @return the build
     */
    public LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = define();
        this.children.forEach(child -> {
            LiteralCommandNode<CommandSourceStack> childNode = child.build();
            root.then(childNode);

            //add all aliases like Paper does for the root
            for (String alias : child.getAliases()) {
                root.then(BrigadierNodes.copyLiteral(alias, childNode));
            }
        });
        return root.requires(this::requirement).build();
    }


    /**
     * Defines the requirements the root command needs to be runnable
     * Default: Command is Enabled, Sender has correct rank. Console may run it.
     * A command that needs a player executor adds {@link #executorIsPlayer(CommandSourceStack)} by overriding this.
     * <p>Used in {@link #build()}</p>
     * @param source the CommandSourceStack
     * @return whether the runner can use the command
     */
    public boolean requirement(CommandSourceStack source) {
        return commandIsEnabled() && senderHasCorrectRank(source);
    }

    /**
     * Outline the requirements to run this command and compare it to the current context
     * @param context the {@link CommandContext} to compare
     * @return the {@link Component} describing this command's requirements
     */
    public Component getRequirementComponent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final boolean canRun = requirement(context.getSource());
        final boolean qualifies = senderHasCorrectRank(context.getSource());
        return Component.empty()
                .append(Translations.component(canRun ? "core.command.requirement.can_run" : "core.command.requirement.cannot_run")
                        .color(canRun ? NamedTextColor.GREEN : NamedTextColor.RED))
                .appendNewline()
                .append(Translations.component("core.command.requirement.enabled",
                        Component.text(this.enabled, this.enabled ? NamedTextColor.GREEN : NamedTextColor.RED)).color(NamedTextColor.WHITE))
                .appendNewline()
                .append(Translations.component("core.command.requirement.rank",
                        Component.text(this.requiredRank.name(), this.requiredRank.getColor())).color(NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Translations.component("core.command.requirement.qualifies",
                        Component.text(qualifies, qualifies ? NamedTextColor.GREEN : NamedTextColor.RED)).color(NamedTextColor.WHITE));
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
        return this.senderHasRank(source, this.requiredRank);
    }

    /**
     * Checks if the {@link CommandSourceStack#getSender()} has the correct {@link Rank} to run this command
     * @param source the {@link CommandSourceStack}
     * @param rank the {@link Rank} to check against
     * @return {@code false} if {@link CommandSourceStack#getSender()} is a {@link Player}
     * who is not op and does not have the required {@link Rank},
     * {@code true} otherwise (i.e. console)
     */
    protected boolean senderHasRank(CommandSourceStack source, Rank rank) {
        if (source.getSender() instanceof final Player sender) {
            return sender.isOp() || clientManager.search().online(sender).hasRank(rank);
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
        final CommandSourceStack source = context.getSource();
        if (!(source.getExecutor() instanceof final Player player)) throw ArgumentException.PLAYER_ONLY.create();
        return player;
    }

    /**
     * Gets the {@link Player} executing this command
     * @param context the {@link CommandContext}
     * @return the {@link Player} executing this command
     * @throws CommandSyntaxException if {@link CommandSourceStack#getSender()} not {@code instanceof} {@link Player}
     */
    @NotNull
    protected Player getPlayerFromSender(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getSender() instanceof final Player player)) throw ArgumentException.PLAYER_ONLY.create();
        return player;
    }

    /**
     * Gets the {@link Client} of the {@link CommandSourceStack#getExecutor()}
     * @param context the {@link CommandContext<CommandSourceStack>}
     * @return the {@link Client}
     * @throws CommandSyntaxException if {@link CommandSourceStack#getExecutor()} is not instance of {@link Player}
     */
    @NotNull
    protected Client getClientFromExecutor(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return clientManager.search().online(getPlayerFromExecutor(context));
    }

    //Since we cannot throw a CommandSyntaxException in async contexts, this will pseudo throw on an empty optional.
    
    /**
     * Gets the requested client by name, or informs the CommandSender that it does not exist
     * If optional is empty, inform player
     * @param name the name of the client
     * @param commandSender the player sending the command
     */
    protected CompletableFuture<Optional<Client>> getOfflineClientByName(String name, CommandSender commandSender) {
        return clientManager.search().offline(name).handle((clientOptional, throwable) -> {
            if (throwable != null) {
                log.error("Error retrieving offline player for command {}", getName(), throwable).submit();
                UtilMessage.sendCommandSyntaxException(commandSender, ArgumentException.OFFLINE_LOOKUP_FAILED.create(name));
                return Optional.empty();
            }
            if (clientOptional.isEmpty()) {
                UtilMessage.sendCommandSyntaxException(commandSender, PlayerNameArgumentType.UNKNOWN_PLAYER_EXCEPTION.create(name));
            }
            return clientOptional;
        });
    }

    /**
     * Get all the usages of this command
     * @param source the {@link CommandSourceStack}
     * @param parentUsage the usage of the parent to this command
     * @return one line per usable path of this command, each prefixed with {@code parentUsage} when given
     */
    public String getUsages(CommandSourceStack source, @Nullable String parentUsage) {
        LiteralCommandNode<CommandSourceStack> node = build();
        String prefix = parentUsage == null ? node.getUsageText() : parentUsage + " " + node.getUsageText();
        final String[] usages = new CommandDispatcher<CommandSourceStack>().getAllUsage(node, source, true);
        if (usages.length == 0) {
            return prefix;
        }
        return Arrays.stream(usages)
                .map(usage -> usage.isEmpty() ? prefix : prefix + " " + usage)
                .collect(Collectors.joining("\n"));
    }
}
