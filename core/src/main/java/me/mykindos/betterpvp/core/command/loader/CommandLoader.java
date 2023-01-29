package me.mykindos.betterpvp.core.command.loader;

import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
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

            String enabledPath = "command." + command.getName().toLowerCase() + ".enabled";
            String rankPath = "command." + command.getName().toLowerCase() + ".requiredRank";

            boolean enabled = plugin.getConfig().getOrSaveBoolean(enabledPath, true);
            Rank rank = Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase());

            command.setEnabled(enabled);
            command.setRequiredRank(rank);

            loadSubCommands(command, "command." + command.getName().toLowerCase() + ".");

            commandManager.addObject(command.getName().toLowerCase(), command);

            count++;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void reload(String packageName) {
        commandManager.getObjects().values().forEach(command -> {
            if(!command.getClass().getPackageName().contains(packageName)) return;
            String enabledPath = "command." + command.getName().toLowerCase() + ".enabled";
            String rankPath = "command." + command.getName().toLowerCase() + ".requiredRank";
            command.setEnabled(plugin.getConfig().getOrSaveBoolean(enabledPath, true));
            command.setRequiredRank(Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase()));
            plugin.getInjector().injectMembers(command);

            loadSubCommands(command, "command." + command.getName().toLowerCase() + ".");

        });
    }

    private void loadSubCommands(ICommand command, String basePath) {
        command.getSubCommands().forEach(subCommand -> {
            String subBasePath = basePath + subCommand.getName() + ".";
            String enabledPath = subBasePath + ".enabled";
            String rankPath = subBasePath + ".requiredRank";

            subCommand.setEnabled(plugin.getConfig().getOrSaveBoolean(enabledPath, true));
            subCommand.setRequiredRank(Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase()));

            plugin.getInjector().injectMembers(subCommand);

            loadSubCommands(subCommand, subBasePath);
        });
    }
}
