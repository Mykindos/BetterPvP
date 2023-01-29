package me.mykindos.betterpvp.core.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
public class CommandManager extends Manager<ICommand> {

    public Optional<ICommand> getCommand(String name) {
        return objects.values().stream()
                .filter(command -> command.getName().equalsIgnoreCase(name) || command.getAliases().contains(name))
                .findFirst();
    }

    public Optional<ICommand> getCommand(String name, String[] args) {
        Optional<ICommand> baseCommand = getCommand(name);
        if (baseCommand.isPresent()) {
            ICommand command = baseCommand.get();
            for (String arg : args) {
                Optional<ICommand> subCommand = command.getSubCommand(arg);
                if (subCommand.isPresent()) {
                    command = subCommand.get();
                } else {
                    break;
                }
            }

            return Optional.of(command);

        }

        return Optional.empty();
    }

    public int getSubCommandIndex(String command, String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(command)) {
                return i;
            }
        }
        return 0;
    }


    public Optional<ICommand> getCommandByAlias(String search) {
        return objects.values().stream().filter(command -> command.getAliases().contains(search)).findFirst();
    }

    @Override
    public void loadFromList(List<ICommand> objects) {
        objects.forEach(command -> addObject(command.getName(), command));
    }
}
