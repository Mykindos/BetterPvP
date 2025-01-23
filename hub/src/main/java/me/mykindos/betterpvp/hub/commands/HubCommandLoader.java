package me.mykindos.betterpvp.hub.commands;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CommandLoader;
import me.mykindos.betterpvp.hub.Hub;
import org.reflections.Reflections;

import java.util.Set;

@CustomLog
public class HubCommandLoader extends CommandLoader {

    @Inject
    public HubCommandLoader(Hub plugin, CommandManager commandManager) {
        super(plugin, commandManager);
    }

    public void loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);

        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        loadAll(classes);

        Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(SubCommand.class);
        loadSubCommands(subCommandClasses);

        plugin.saveConfig();
        log.info("Loaded {} commands for Hub", count).submit();
    }

}
