package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import org.reflections.Reflections;

import java.util.Set;

@Singleton
@CustomLog
public class BrigadierCoreCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierCoreCommandLoader(Core plugin) {
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
