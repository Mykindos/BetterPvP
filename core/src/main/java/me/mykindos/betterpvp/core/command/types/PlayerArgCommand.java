package me.mykindos.betterpvp.core.command.types;

import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class PlayerArgCommand extends Command {

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {

        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
