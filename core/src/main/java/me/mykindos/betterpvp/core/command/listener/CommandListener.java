package me.mykindos.betterpvp.core.command.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.ISubCommand;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Optional;

@BPvPListener
public class CommandListener implements Listener {

    private final ClientManager clientManager;
    private final CommandManager commandManager;

    @Inject
    public CommandListener(ClientManager clientManager, CommandManager commandManager) {
        this.clientManager = clientManager;
        this.commandManager = commandManager;
    }


    @EventHandler
    public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
        Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId().toString());
        if (clientOptional.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        Client client = clientOptional.get();
        String commandName = event.getMessage().substring(1).toLowerCase();

        if (commandName.contains(" ")) {
            commandName = commandName.split(" ")[0];
        }

        String finalCommandName = commandName;
        Optional<ICommand> commandOptional = commandManager.getObject(commandName)
                .or(() -> commandManager.getCommandByAlias(finalCommandName));
        if (commandOptional.isEmpty() && !client.hasRank(Rank.ADMIN) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            return;
        }

        commandOptional.ifPresent(command -> {
            if (command.isEnabled()) {
                if (client.hasRank(command.getRequiredRank()) || event.getPlayer().isOp()) {
                    String[] args = event.getMessage().substring(event.getMessage().indexOf(' ') + 1).split(" ");

                    // Execute a subcommand directly if available
                    var subCommandOptional = getSubCommand(command, args);
                    if(subCommandOptional.isEmpty()){
                        command.execute(event.getPlayer(), client, args);
                    } else{
                        ISubCommand subCommand = subCommandOptional.get();
                        String[] newArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[]{};
                        subCommand.execute(event.getPlayer(), client, newArgs);
                    }
                    event.setCancelled(true);
                } else {
                    if (command.informInsufficientRank()) {
                        System.out.println("Insufficient permissions for " + command.getName());
                        // Message invalid permissions
                    }
                }
            } else {
                System.out.println(command.getName() + " is disabled");
                event.setCancelled(true);
            }
        });

    }


    private Optional<ISubCommand> getSubCommand(ICommand command, String[] args) {
        if (args.length > 0) {
            String arg = args[0];
            return command.getSubCommands().stream().filter(sub -> sub.getName().equalsIgnoreCase(arg)).findFirst();
        } else {
            return Optional.empty();
        }
    }
}
