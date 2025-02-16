package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Singleton;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

import java.lang.reflect.Modifier;
import java.util.Set;

@Singleton
@CustomLog
public class BrigadierCommandLoader extends Loader {
    public BrigadierCommandLoader(BPvPPlugin plugin) {
        super(plugin);
    }

    @Override
    public void load(Class<?> clazz) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            try {

                BrigadierCommand brigadierCommand = (BrigadierCommand) plugin.getInjector().getInstance(clazz);
                plugin.getInjector().injectMembers(brigadierCommand);

                //TODO move into BrigadierCommand, add configs for all sub-commands/arguments
                String enabledPath = "command." + brigadierCommand.getName().toLowerCase() + ".enabled";
                String rankPath = "command." + brigadierCommand.getName().toLowerCase() + ".requiredRank";

                boolean enabled = plugin.getConfig().getOrSaveBoolean(enabledPath, true);
                Rank rank = Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase());

                brigadierCommand.setEnabled(enabled);
                brigadierCommand.setRequiredRank(rank);

                //tempCommands.add(command);
                //commandManager.addObject(command.getName().toLowerCase(), command);
                commands.registrar().register(brigadierCommand.build(), brigadierCommand.getDescription(), brigadierCommand.getAliases());
                log.info("Loaded brigadier command {}", brigadierCommand.getName()).submit();
                this.plugin.saveConfig();

            } catch (Exception ex) {
                log.error("Failed to load command", ex);
            }

        });
    }

    public void loadAll(Set<Class<? extends BrigadierCommand>> classes) {
        for (var clazz : classes) {
            log.info(clazz.getName()).submit();
            if (BrigadierCommand.class.isAssignableFrom(clazz)) {
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    load(clazz);
                }
            }
        }
    }
}
