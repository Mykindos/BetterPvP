package me.mykindos.betterpvp.core.command;


import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpigotCommandWrapper extends org.bukkit.command.Command {

    @Inject
    private ClientManager clientManager;

    private final Command command;

    public SpigotCommandWrapper(Command command, @NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases) {
        super(name, description, usageMessage, aliases);
        this.command = command;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args, @Nullable Location location) {
        List<String> aliases = new ArrayList<>();

        if (args.length == 0) return new ArrayList<>();

        if (sender instanceof Player player) {
            Client client = clientManager.search(sender).inform(false).online(player);
            if (!client.hasRank(this.command.getRequiredRank()) && !sender.isOp()) {
                return aliases;
            }
        }

        if (command.getArgumentType(1).equals(ICommand.ArgumentType.SUBCOMMAND.name())) {
            Optional<ICommand> subCommandOptional = command.getSubCommand(args[0]);
            if (subCommandOptional.isPresent()) {
                ICommand subCommand = subCommandOptional.get();
                if (subCommand.showTabCompletion(sender)) {
                    return subCommand.processTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
                } else {
                    return new ArrayList<>();
                }
            }
        }

        return this.command.processTabComplete(sender, args);
    }
}
