package me.mykindos.betterpvp.core.command;

import lombok.Setter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public abstract class Command implements ICommand {

    @Setter
    private boolean enabled;

    protected List<String> aliases;
    protected List<SubCommand> subCommands;

    public Command() {
        aliases = new ArrayList<>();
        subCommands = new ArrayList<>();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 1) {
            getSubCommands().forEach(subCommand -> {
                tabCompletions.add(subCommand.getName());
                tabCompletions.addAll(subCommand.getAliases());
            });
        }

        return tabCompletions;
    }

}
