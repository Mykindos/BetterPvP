package me.mykindos.betterpvp.core.command.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.reflections.Reflections;

@Singleton
@CustomLog
public class BrigadierCommandLoader extends Loader {

    @Inject
    private BrigadierCommandManager brigadierCommandManager;

    private final Map<Class<?>, BrigadierCommand> loadedCommands = new HashMap<>();

    public BrigadierCommandLoader(BPvPPlugin plugin) {
        super(plugin);
    }

    /**
     * Loads every top-level Brigadier command in {@code packageName} and attaches the subcommands annotated with
     * {@link BrigadierSubCommand} to their parents.
     */
    public void loadCommands(String packageName) {
        final Reflections reflections = new Reflections(packageName);
        loadAll(reflections.getSubTypesOf(BrigadierCommand.class));
        loadSubCommands(reflections.getTypesAnnotatedWith(BrigadierSubCommand.class));
    }

    @Override
    public void load(Class<?> clazz) {
        register(List.of(clazz));
    }

    /**
     * Registers the commands once Paper fires its commands event, which runs after all plugins are loaded, then saves
     * the defaults their config wrote.
     */
    private void register(Collection<? extends Class<?>> classes) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            classes.forEach(clazz -> register(commands.registrar(), clazz));
            plugin.saveConfig();
        });
    }

    private void register(Commands registrar, Class<?> clazz) {
        try {
            BrigadierCommand brigadierCommand = (BrigadierCommand) plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(brigadierCommand);

            brigadierCommand.setConfig(plugin.getConfig("permissions/commands"));
            LiteralCommandNode<CommandSourceStack> built = brigadierCommand.build();
            // Paper registers the description as plain text for every viewer, so it is resolved to English here.
            final String description = PlainTextComponentSerializer.plainText()
                    .serialize(Translations.render(brigadierCommand.getDescriptionComponent(), (Locale) null));
            registrar.register(built, description, brigadierCommand.getAliases());
            log.info("Loaded brigadier command {}", brigadierCommand.getName()).submit();

            loadedCommands.put(clazz, brigadierCommand);
            brigadierCommandManager.addObject(built.getName(), brigadierCommand);
            //because paper registers new commands for each alias, we need to add the alias too
            brigadierCommand.getAliases().forEach(alias -> {
                brigadierCommandManager.addObject(alias, brigadierCommand);
            });
        } catch (Exception ex) {
            log.error("Failed to load command", ex).submit();
        }
    }

    public void loadSubCommands(Set<Class<?>> classes) {
        log.info(Arrays.toString(classes.toArray())).submit();
        classes.forEach(clazz -> {
                    BrigadierSubCommand subCommandAnnotation = clazz.getAnnotation(BrigadierSubCommand.class);
                    BrigadierCommand parent = plugin.getInjector().getInstance(subCommandAnnotation.value());
                    BrigadierCommand subCommand = (BrigadierCommand) plugin.getInjector().getInstance(clazz);
                    plugin.getInjector().injectMembers(subCommand);
                    log.info("Adding Brigadier Sub Command {} to {}", subCommand.getName(), parent.getName()).submit();
                    subCommand.setParent(parent);
                    parent.getChildren().add(subCommand);
                });
    }

    private void loadAll(Set<Class<? extends BrigadierCommand>> classes) {
        register(classes.stream()
                .filter(clazz -> !clazz.isAnnotationPresent(BrigadierSubCommand.class))
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .toList());
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
