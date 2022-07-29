package me.mykindos.betterpvp.core.command.loader;

import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.Loader;

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
            plugin.getListeners().add(command);


            var commandPath = "command." + command.getName() + ".enabled";
            var set = plugin.getConfig().isSet(commandPath.toLowerCase());
            if (!set) {
                plugin.getConfig().set(commandPath.toLowerCase(), true);
            }

            command.setEnabled(plugin.getConfig().getBoolean(commandPath.toLowerCase()));
            commandManager.addObject(command.getName().toLowerCase(), command);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
