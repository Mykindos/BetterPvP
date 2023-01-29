package me.mykindos.betterpvp.champions.commands;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CommandLoader;
import org.reflections.Reflections;

import java.util.Set;

@Slf4j
public class ChampionsCommandLoader extends CommandLoader {


    @Inject
    public ChampionsCommandLoader(Champions plugin, CommandManager commandManager) {
        super(plugin, commandManager);
    }

    public void loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        for (var clazz : classes) {
            if (Command.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(SubCommand.class)) {
                load(clazz);
            }
        }

        plugin.saveConfig();
        log.info("Loaded " + count + " commands for Champions");
    }
}
