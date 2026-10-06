package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;

@Singleton
@CustomLog
public class BrigadierCommandLoader extends Loader {

    @Inject
    private BrigadierCommandManager brigadierCommandManager;

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

                brigadierCommand.setConfig(plugin.getConfig("permissions/commands"));
                LiteralCommandNode<CommandSourceStack> built = brigadierCommand.build();
                commands.registrar().register(built, brigadierCommand.getDescription(), brigadierCommand.getAliases());
                log.info("Loaded brigadier command {}", brigadierCommand.getName()).submit();
                plugin.saveConfig();

                brigadierCommandManager.addObject(built.getName(), brigadierCommand);
                //because paper registers new commands for each alias, we need to add the alias too
                brigadierCommand.getAliases().forEach(alias -> {
                    brigadierCommandManager.addObject(alias, brigadierCommand);
                });
            } catch (Exception ex) {
                log.error("Failed to load command", ex).submit();
            }

        });
    }

    public void loadSubCommands(Set<Class<?>> classes) {
        log.info(Arrays.toString(classes.toArray())).submit();
        classes.forEach(clazz -> {
                    BrigadierSubCommand subCommandAnnotation = clazz.getAnnotation(BrigadierSubCommand.class);
                    IBrigadierCommand parent = plugin.getInjector().getInstance(subCommandAnnotation.value());
                    IBrigadierCommand subCommand = (IBrigadierCommand) plugin.getInjector().getInstance(clazz);
                    plugin.getInjector().injectMembers(subCommand);
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

    @Override
    public void reload(String packageName) {
        this.reload();
    }

    /**
     * @see net.minecraft.server.ReloadableServerResources#loadResources(ResourceManager, LayeredRegistryAccess, List, FeatureFlagSet, Commands.CommandSelection, int, Executor, Executor)
     */
    public void reload() {
        //Does not affect configs

        /*brigadierCommandManager.getObjects().values().forEach(command -> {
            if (!command.getClass().getPackageName().contains(plugin.getClass().getPackageName())) return;
            command.setConfig(plugin.getConfig("permissions/commands"));
            plugin.saveConfig();
        });
        io.papermc.paper.command.brigadier.PaperCommands.INSTANCE.setValid();
        io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner.INSTANCE.callReloadableRegistrarEvent(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                io.papermc.paper.command.brigadier.PaperCommands.INSTANCE,
                plugin.getClass(),
                io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.Cause.RELOAD);
        io.papermc.paper.command.brigadier.PaperCommands.INSTANCE.invalidate();*/

    }
}
