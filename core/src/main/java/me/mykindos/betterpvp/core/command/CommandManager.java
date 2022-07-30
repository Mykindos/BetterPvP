package me.mykindos.betterpvp.core.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.manager.Manager;

import java.util.List;
import java.util.Optional;

@Singleton
public class CommandManager extends Manager<Command> {

    public Optional<Command> getCommandByAlias(String search) {
        return objects.values().stream().filter(command -> command.getAliases().contains(search)).findFirst();
    }

    @Override
    public void loadFromList(List<Command> objects) {
        objects.forEach(command -> addObject(command.getName(), command));
    }
}
