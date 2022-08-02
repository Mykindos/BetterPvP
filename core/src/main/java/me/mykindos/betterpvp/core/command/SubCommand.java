package me.mykindos.betterpvp.core.command;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public abstract class SubCommand implements ICommand {


    protected List<String> aliases;

    public SubCommand(){
        aliases = new ArrayList<>();
    }

    @Setter
    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        switch (getArgumentType(args.length)) {
            case PLAYER -> tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                    startsWith(args[args.length-1].toLowerCase())).toList());
            case POSITION_X -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getX() + "" : "0");
            case POSITION_Y -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getY() + "" : "0");
            case POSITION_Z -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getZ() + "" : "0");
        }


        return tabCompletions;

    }

}