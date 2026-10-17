package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

@Singleton
@CustomLog
public class BrigadierCommandLoader extends Loader {

    @Inject
    private BrigadierCommandManager brigadierCommandManager;

    private final Map<Class<?>, BrigadierCommand> loadedCommands = new HashMap<>();

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
                // Paper registers the description as plain text for every viewer, so it is resolved to English here.
                final String description = PlainTextComponentSerializer.plainText()
                        .serialize(Translations.render(brigadierCommand.getDescriptionComponent(), (Locale) null));
                commands.registrar().register(built, description, brigadierCommand.getAliases());
                log.info("Loaded brigadier command {}", brigadierCommand.getName()).submit();
                plugin.saveConfig();

                loadedCommands.put(clazz, brigadierCommand);
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
     * Re-reads {@code permissions/commands} for every command this loader loaded, so a changed rank or enabled flag
     * applies without a restart. Players see the change once their command tree is resent.
     */
    public void reload() {
        ExtendedYamlConfiguration config = plugin.getConfig("permissions/commands");
        loadedCommands.values().forEach(command -> command.setConfig(config));
        plugin.saveConfig();
    }
}
