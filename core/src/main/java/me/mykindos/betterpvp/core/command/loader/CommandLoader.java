package me.mykindos.betterpvp.core.command.loader;

import me.mykindos.betterpvp.core.client.Rank;
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

                String enabledPath = "command." + command.getName().toLowerCase() + "." + sub.getName().toLowerCase() + ".enabled";
                String rankPath = "command." + command.getName().toLowerCase() + "." + sub.getName().toLowerCase() + ".requiredRank";

                boolean enabled = plugin.getConfig().getOrSaveBoolean(enabledPath, true);
                Rank rank = Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase());

                sub.setEnabled(enabled);
                sub.setRequiredRank(rank);

            });

            String enabledPath = "command." + command.getName().toLowerCase() + ".enabled";
            String rankPath = "command." + command.getName().toLowerCase() + ".requiredRank";

            boolean enabled = plugin.getConfig().getOrSaveBoolean(enabledPath, true);
            Rank rank = Rank.valueOf(plugin.getConfig().getOrSaveString(rankPath, "ADMIN").toUpperCase());

            command.setEnabled(enabled);
            command.setRequiredRank(rank);

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
            command.getSubCommands().forEach(subCommand -> {
                String subEnabledPath = "command." + command.getName().toLowerCase() + "." + subCommand.getName().toLowerCase() + ".enabled";
                String subRankPath = "command." + command.getName().toLowerCase() + "." + subCommand.getName().toLowerCase() + ".requiredRank";
                subCommand.setEnabled(plugin.getConfig().getOrSaveBoolean(subEnabledPath, true));
                subCommand.setRequiredRank(Rank.valueOf(plugin.getConfig().getOrSaveString(subRankPath, "ADMIN").toUpperCase()));

                plugin.getInjector().injectMembers(subCommand);
            });
        });
    }
}
