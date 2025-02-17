package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Singleton;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;

@Singleton
@CustomLog
public class BrigadierCommandLoader extends Loader {
    public BrigadierCommandLoader(BPvPPlugin plugin) {
        super(plugin);
    }

    @Override
    public void load(Class<?> clazz) {
        //this registration event runs after all plugins are loaded
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            try {

                BrigadierCommand brigadierCommand = (BrigadierCommand) plugin.getInjector().getInstance(clazz);
                plugin.getInjector().injectMembers(brigadierCommand);

                brigadierCommand.setConfig(plugin.getConfig("commands"));

                commands.registrar().register(brigadierCommand.build(), brigadierCommand.getDescription(), brigadierCommand.getAliases());
                log.info("Loaded brigadier command {}", brigadierCommand.getName()).submit();
                plugin.saveConfig();

            } catch (Exception ex) {
                log.error("Failed to load command", ex).submit();
            }

        });
    }

    public void loadSubCommands(Set<Class<?>> classes) {
        log.info(Arrays.toString(classes.toArray())).submit();
        classes.stream()
                /*.filter(clazz -> {
                    log.info("IBrig").submit();
                    return clazz.isAssignableFrom(IBrigadierCommand.class);
                })
                .filter(clazz -> {
                    log.info("annotation").submit();
                    return clazz.isAnnotationPresent(BrigadierSubCommand.class);
                })*/
                .forEach(clazz -> {
                    BrigadierSubCommand subCommandAnnotation = clazz.getAnnotation(BrigadierSubCommand.class);
                    IBrigadierCommand parent = plugin.getInjector().getInstance(subCommandAnnotation.value());
                    IBrigadierCommand subCommand = (IBrigadierCommand) plugin.getInjector().getInstance(clazz);
                    plugin.getInjector().injectMembers(subCommand);
                    subCommand.setConfig(plugin.getConfig("commands"));
                    log.info("Adding Brigadier Sub Command {} to {}", subCommand.getName(), parent.getName()).submit();
                    subCommand.setParent(parent);
                    parent.getChildren().add(subCommand);
                });
    }

    public void loadAll(Set<Class<? extends IBrigadierCommand>> classes) {
        for (var clazz : classes) {
            if (BrigadierCommand.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(BrigadierSubCommand.class)) {
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    load(clazz);
                }
            }
        }
    }
}
