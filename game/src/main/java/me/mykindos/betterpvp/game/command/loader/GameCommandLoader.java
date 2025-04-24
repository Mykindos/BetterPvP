package me.mykindos.betterpvp.game.command.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.command.loader.CommandLoader;
import me.mykindos.betterpvp.game.GamePlugin;
import org.reflections.Reflections;

import java.util.Set;

@CustomLog
@Singleton
public class GameCommandLoader extends CommandLoader {

    @Inject
    public GameCommandLoader(GamePlugin plugin, CommandManager commandManager) {
        super(plugin, commandManager);
    }

    public void loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);

        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        loadAll(classes);

        Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(SubCommand.class);
        loadSubCommands(subCommandClasses);

        plugin.saveConfig();
        log.info("Loaded {} commands for Game", count).submit();;
    }

}
