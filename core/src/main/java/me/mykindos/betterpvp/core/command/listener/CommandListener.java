package me.mykindos.betterpvp.core.command.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Arrays;
import java.util.Optional;

@CustomLog
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
    public void onServerCommand(ServerCommandEvent event) {
        log.info("Server executed command: {}", event.getCommand()).submit();
        String commandName = event.getCommand().toLowerCase();
        if (commandName.contains(" ")) {
            commandName = commandName.split(" ")[0];
        }

        String[] args = event.getCommand().substring(event.getCommand().indexOf(' ') + 1).split(" ");
        if (args[0].equalsIgnoreCase(event.getCommand())) args = new String[]{};
        String[] finalArgs = args;

        Optional<ICommand> commandOptional = commandManager.getCommand(commandName, finalArgs);
        if (commandOptional.isPresent()) {
            ICommand command = commandOptional.get();
            if(!(command instanceof IConsoleCommand consoleCommand)) return;
            if (!command.isEnabled()) {
                log.info("Console attempted to use " + command.getName() + " but it is disabled").submit();
                return;
            }

            if (command.getClass().isAnnotationPresent(SubCommand.class)) {
                int subCommandIndex = commandManager.getSubCommandIndex(commandName, finalArgs);
                finalArgs = finalArgs.length > 1 ? Arrays.copyOfRange(finalArgs, subCommandIndex + 1, finalArgs.length) : new String[]{};
            }

            consoleCommand.execute(event.getSender(), finalArgs);

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        String commandName = event.getMessage().substring(1).toLowerCase();

        log.info("{} executed command: {}", event.getPlayer().getName(), event.getMessage()).submit();

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
                log.info(event.getPlayer().getName() + " attempted to use " + command.getName() + " but it is disabled").submit();
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

    @EventHandler
    public void onCommandListSent(PlayerCommandSendEvent event) {
        Client client = clientManager.search().online(event.getPlayer());

        event.getCommands().removeIf(command -> {

            String[] args = command.split(":");
            if(args.length == 2) {
                return args[0].equalsIgnoreCase(args[1]);
            }

            return false;
        });

        if(event.getPlayer().isOp() || client.hasRank(Rank.ADMIN)) return;


        event.getCommands().removeIf(command -> {

            Optional<ICommand> commandOptional = commandManager.getCommand(command, new String[]{});
            if (commandOptional.isPresent()) {
                ICommand command1 = commandOptional.get();
                return !client.hasRank(command1.getRequiredRank()) && !event.getPlayer().isOp();
            }

            return true;
        });
    }

    private void promptInsufficientPrivileges(ICommand command, Player player) {
        if (command.informInsufficientRank()) {
            UtilMessage.message(player, "Command", "You have insufficient privileges to perform this command.");
        }
    }

}
