package me.mykindos.betterpvp.core.command.loader;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import org.reflections.Reflections;

import java.util.Set;

public class CoreCommandLoader extends CommandLoader{


    @Inject
    public CoreCommandLoader(Core plugin, CommandManager commandManager) {
        super(plugin, commandManager);
    }

    public void loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        for (var clazz : classes) {
            if (Command.class.isAssignableFrom(clazz)) {
                load(clazz);
            }
        }

        plugin.saveConfig();

        System.out.println("Loaded " + count + " for Core");
    }

}
