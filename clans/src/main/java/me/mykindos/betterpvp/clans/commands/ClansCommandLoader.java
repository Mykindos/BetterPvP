package me.mykindos.betterpvp.clans.commands;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CommandLoader;
import org.reflections.Reflections;

import java.util.Set;

@Slf4j
public class ClansCommandLoader extends CommandLoader {


    @Inject
    public ClansCommandLoader(Clans plugin, CommandManager commandManager) {
        super(plugin, commandManager);
    }

    public void loadCommands(String packageName) {

        Reflections reflections = new Reflections(packageName);

        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        loadAll(classes);

        Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(SubCommand.class);
        loadSubCommands(subCommandClasses);

        plugin.saveConfig();
        log.info("Loaded {} commands for Clans", count);
    }

}
