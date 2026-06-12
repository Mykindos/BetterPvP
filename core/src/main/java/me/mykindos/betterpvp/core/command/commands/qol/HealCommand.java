package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class HealCommand extends Command {

    @Override
    public String getName() {
        return "heal";
    }

    @Override
    public String getDescription() {
        return "core.command.heal.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            player.setHealth(UtilPlayer.getMaxHealth(player));
            UtilMessage.message(player, "core.prefix.heal", "core.command.heal.self.success");
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                target.setHealth(UtilPlayer.getMaxHealth(target));
                UtilMessage.message(player, "core.prefix.heal", "core.command.heal.other.success", Component.text(target.getName()));
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
