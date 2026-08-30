package me.mykindos.betterpvp.shops.commands.loader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommandLoader;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.shops.Shops;
import org.reflections.Reflections;

import java.util.Set;

@Singleton
@CustomLog
public class BrigadierShopsCommandLoader extends BrigadierCommandLoader {
    @Inject
    public BrigadierShopsCommandLoader(Shops plugin) {
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
