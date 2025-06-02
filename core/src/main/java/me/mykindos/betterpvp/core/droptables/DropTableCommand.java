package me.mykindos.betterpvp.core.droptables;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
@CustomLog
public class DropTableCommand extends Command {
    @Override
    public String getName() {
        return "droptable";
    }

    @Override
    public String getDescription() {
        return "View a specific droptable";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.simpleMessage(player, "Command", "Usage: /droptable <name>");
            return;
        }

        DropTable dropTable = DropTable.dropTableRegistry.get(args[0]);
        if(dropTable == null) {
            UtilMessage.simpleMessage(player, "Command", "No droptable found with that name");
            return;
        }

        log.info("{} opened the droptable for {}", player.getName(), args[0]).submit();
        dropTable.showInventory(player);
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DropTable.dropTableRegistry.keySet().stream().filter(name -> name.startsWith(args[0])).toList();
        } else {
            return super.processTabComplete(sender, args);
        }
    }
}
