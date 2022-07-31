package me.mykindos.betterpvp.core.command.loader;

import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.SpigotCommandWrapper;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;
import org.bukkit.Bukkit;

public class CommandLoader extends Loader {

    protected final CommandManager commandManager;

    public CommandLoader(BPvPPlugin plugin, CommandManager commandManager) {
        super(plugin);
        this.commandManager = commandManager;
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

            command.getSubCommands().forEach(sub -> {
                plugin.getInjector().injectMembers(sub);

                var subCommandPath = "command." + command.getName().toLowerCase() + "." + sub.getName().toLowerCase() + ".enabled";
                addEnabledToConfig(subCommandPath);
                sub.setEnabled(plugin.getConfig().getBoolean(subCommandPath));
            });

            var commandPath = "command." + command.getName().toLowerCase() + ".enabled";
            addEnabledToConfig(commandPath);

            command.setEnabled(plugin.getConfig().getBoolean(commandPath));
            commandManager.addObject(command.getName().toLowerCase(), command);

            count++;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void reload() {
        commandManager.getObjects().values().forEach(command -> {
            var commandPath = "command." + command.getName().toLowerCase() + ".enabled";
            command.setEnabled(plugin.getConfig().getBoolean(commandPath));
            plugin.getInjector().injectMembers(command);
            command.getSubCommands().forEach(subCommand -> {
                plugin.getInjector().injectMembers(plugin);
            });
        });
    }

    private void addEnabledToConfig(String path) {
        var set = plugin.getConfig().isSet(path);
        if (!set) {
            plugin.getConfig().set(path, true);
        }
    }
}
