package me.mykindos.betterpvp.core.command.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
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

        String[] args = event.getMessage().substring(event.getMessage().indexOf(' ') + 1).split(" ");
        if (args[0].equalsIgnoreCase(event.getMessage())) args = new String[]{};
        String[] finalArgs = args;

        String finalCommandName = commandName;
        Optional<ICommand> commandOptional = commandManager.getCommand(finalCommandName, finalArgs);
        if (commandOptional.isEmpty() && !client.hasRank(Rank.ADMIN) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            return;
        }

        if (commandOptional.isPresent()) {
            ICommand command = commandOptional.get();

            if (!command.isEnabled()) {
                log.info(event.getPlayer().getName() + " attempted to use " + command.getName() + " but it is disabled");
                return;
            }

            if (!client.hasRank(command.getRequiredRank()) && !event.getPlayer().isOp()) {
                promptInsufficientPrivileges(command, event.getPlayer());
                return;
            }

            if (command.getClass().isAnnotationPresent(SubCommand.class)) {
                int subCommandIndex = commandManager.getSubCommandIndex(finalCommandName, finalArgs);
                finalArgs = finalArgs.length > 1 ? Arrays.copyOfRange(finalArgs, subCommandIndex + 1, finalArgs.length) : new String[]{};
            }

            command.process(event.getPlayer(), client, finalArgs);

            event.setCancelled(true);
        }
    }

    private void promptInsufficientPrivileges(ICommand command, Player player) {
        if (command.informInsufficientRank()) {
            UtilMessage.message(player, "Command", "You have insufficient privileges to perform this command.");
        }
    }

}
