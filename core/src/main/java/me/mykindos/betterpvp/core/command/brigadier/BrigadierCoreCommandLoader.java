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

    public void loadCommands(Core core) {
        Reflections reflections = new Reflections(core.getPACKAGE());
        Set<Class<? extends BrigadierCommand>> classes = reflections.getSubTypesOf(BrigadierCommand.class);
        loadAll(classes);

        plugin.saveConfig();
    }
}
