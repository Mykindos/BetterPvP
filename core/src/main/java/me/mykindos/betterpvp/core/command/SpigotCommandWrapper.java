package me.mykindos.betterpvp.core.command;


import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

        if (sender instanceof Player player) {
            Optional<Client> clientOptional = clientManager.getObject(player.getUniqueId().toString());
            if (clientOptional.isPresent()) {
                if (!clientOptional.get().hasRank(this.command.getRequiredRank())) {
                    return aliases;
                }
            }
        }

        if (args.length == 1) {
            this.command.getSubCommands().forEach(subCommand -> {
                aliases.add(subCommand.getName());
                aliases.addAll(subCommand.getAliases());
            });

        }

        return aliases;
    }
}
