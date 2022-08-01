package me.mykindos.betterpvp.core.command;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        switch (getArgumentType(args.length)) {
            case SUBCOMMAND -> getSubCommands().forEach(subCommand -> {
                tabCompletions.add(subCommand.getName());
                tabCompletions.addAll(subCommand.getAliases());
            });
            case PLAYER -> tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            case POSITION_X -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getX() + "" : "0");
            case POSITION_Y -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getY() + "" : "0");
            case POSITION_Z -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getZ() + "" : "0");
        }


        return tabCompletions;
    }

    public ArgumentType getArgumentType(int argCount) {
        return ArgumentType.SUBCOMMAND;
    }

    protected enum ArgumentType {
        NONE,
        SUBCOMMAND,
        PLAYER,
        POSITION_X,
        POSITION_Y,
        POSITION_Z
    }

}
