package me.mykindos.betterpvp.core.command.loader;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.command.SpigotCommandWrapper;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import org.bukkit.Bukkit;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CustomLog
public class CommandLoader extends Loader {

    protected final CommandManager commandManager;
    protected final List<ICommand> tempCommands;

    public CommandLoader(BPvPPlugin plugin, CommandManager commandManager) {
        super(plugin);
        this.commandManager = commandManager;
        this.tempCommands = new ArrayList<>();
    }

    public void loadAll(Set<Class<? extends Command>> classes) {
        for (var clazz : classes) {
            if (Command.class.isAssignableFrom(clazz) && !clazz.isAnnotationPresent(SubCommand.class)) {
                if (!Modifier.isAbstract(clazz.getModifiers())) {
                    load(clazz);
                }
            }
        }
    }

    public void loadSubCommands(Set<Class<?>> classes) {
        for (var clazz : classes) {
            SubCommand subCommandAnnotation = clazz.getAnnotation(SubCommand.class);
            ICommand command = plugin.getInjector().getInstance(subCommandAnnotation.value());
            ICommand subCommand = (ICommand) plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(subCommand);
            log.info("Added {} to {} sub commands", subCommand.getName(), command.getName()).submit();
            command.getSubCommands().add(subCommand);

        }

        tempCommands.forEach(command -> loadSubCommandsConfig(command, "command." + command.getName().toLowerCase() + "."));
        tempCommands.clear();
    }

    @Override
    public void load(Class<?> clazz) {
        try {
            Command command = (Command) plugin.getInjector().getInstance(clazz);
            plugin.getInjector().injectMembers(command);


            SpigotCommandWrapper commandWrapper = new SpigotCommandWrapper(command, command.getName(),
                    command.getDescription(), "", command.getAliases());
            plugin.getInjector().injectMembers(commandWrapper);
            Bukkit.getCommandMap().register(command.getName(), commandWrapper);

            String enabledPath = "command." + command.getName().toLowerCase() + ".enabled";
            String rankPath = "command." + command.getName().toLowerCase() + ".requiredRank";

            boolean enabled = plugin.getConfig().getOrSaveBoolean(enabledPath, true);
            Rank rank = Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase());

            command.setEnabled(enabled);
            command.setRequiredRank(rank);

            tempCommands.add(command);
            commandManager.addObject(command.getName().toLowerCase(), command);

            count++;
        } catch (Exception ex) {
            log.error("Failed to load command", ex);
        }
    }

    @Override
    public void reload(String packageName) {
        commandManager.getObjects().values().forEach(command -> {
            if (!command.getClass().getPackageName().contains(packageName)) return;
            String enabledPath = "command." + command.getName().toLowerCase() + ".enabled";
            String rankPath = "command." + command.getName().toLowerCase() + ".requiredRank";
            command.setEnabled(plugin.getConfig().getOrSaveBoolean(enabledPath, true));
            command.setRequiredRank(Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase()));

            loadSubCommandsConfig(command, "command." + command.getName().toLowerCase() + ".");

        });
    }

    protected void loadSubCommandsConfig(ICommand command, String basePath) {
        command.getSubCommands().forEach(subCommand -> {
            String subBasePath = basePath + subCommand.getName() + ".";
            String enabledPath = subBasePath + ".enabled";
            String rankPath = subBasePath + ".requiredRank";

            subCommand.setEnabled(plugin.getConfig().getOrSaveBoolean(enabledPath, true));
            subCommand.setRequiredRank(Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase()));

            plugin.getInjector().injectMembers(subCommand);

            loadSubCommandsConfig(subCommand, subBasePath);
        });
    }
}
