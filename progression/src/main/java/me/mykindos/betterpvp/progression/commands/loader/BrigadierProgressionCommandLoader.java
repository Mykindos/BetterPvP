package me.mykindos.betterpvp.progression.commands.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.progression.Progression;
import org.reflections.Reflections;

import java.util.Set;

@Singleton
@CustomLog
public class BrigadierProgressionCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierProgressionCommandLoader(Progression plugin) {
        super(plugin);
    }

    public void loadCommands(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IBrigadierCommand>> classes = reflections.getSubTypesOf(IBrigadierCommand.class);
        Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(BrigadierSubCommand.class);
        loadAll(classes);
        loadSubCommands(subCommandClasses);
        plugin.saveConfig();
    }
}
