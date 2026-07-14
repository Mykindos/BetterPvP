package me.mykindos.betterpvp.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import org.reflections.Reflections;

import java.util.Set;

@Singleton
public class BrigadierClansCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierClansCommandLoader(Clans plugin) {
        super(plugin);
    }

    public void loadCommands(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<? extends IBrigadierCommand>> classes = reflections.getSubTypesOf(IBrigadierCommand.class);
        final Set<Class<?>> subCommandClasses = reflections.getTypesAnnotatedWith(BrigadierSubCommand.class);
        loadAll(classes);
        loadSubCommands(subCommandClasses);
    }
}
