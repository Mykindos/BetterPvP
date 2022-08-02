package me.mykindos.betterpvp.clans.clans.commands;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class ClanSubCommand extends SubCommand {

    protected final ClanManager clanManager;

    public ClanSubCommand(ClanManager clanManager){
        this.clanManager = clanManager;
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
            case CLAN -> clanManager.getObjects().forEach((key, value) -> {
                if(key.toLowerCase().startsWith(args[args.length-1].toLowerCase())) {
                    tabCompletions.add(key);
                }
            });
        }


        return tabCompletions;

    }
}
