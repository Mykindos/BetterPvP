package me.mykindos.betterpvp.core.command.commands.qol;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class HealCommand extends Command {
    @Override
    public String getName() {
        return "heal";
    }

    @Override
    public String getDescription() {
        return "Heal yourself or another player to full health";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            player.setHealth(UtilPlayer.getMaxHealth(player));
            UtilMessage.message(player, "Heal", "Successfully healed to full health.");
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                target.setHealth(UtilPlayer.getMaxHealth(target));
                UtilMessage.simpleMessage(player, "Heal", "Successfully healed <yellow>%s<gray> to full health.", target.getName());
            }
        }
    }

    @Override
    public String getArgumentType(int args) {
        if(args == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }
}
