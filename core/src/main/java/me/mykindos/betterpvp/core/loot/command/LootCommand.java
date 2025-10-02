package me.mykindos.betterpvp.core.loot.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.menu.LootTableMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
@CustomLog
public class LootCommand extends Command {

    private final LootTableRegistry lootTableRegistry;

    @Inject
    private LootCommand(LootTableRegistry lootTableRegistry) {
        this.lootTableRegistry = lootTableRegistry;
        this.aliases.add("droptable");
        this.aliases.add("loottable");
    }

    @Override
    public String getName() {
        return "loot";
    }

    @Override
    public String getDescription() {
        return "View a specific loot table";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.simpleMessage(player, "Command", "Usage: /loot <name>");
            return;
        }

        LootTable table = lootTableRegistry.getLoaded().get(args[0]);
        if (table == null) {
            UtilMessage.simpleMessage(player, "Command", "No loot table found with that name");
            return;
        }

        log.info("{} opened the loot table for {}", player.getName(), args[0]).submit();
        new LootTableMenu(table, null).show(player);
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(lootTableRegistry.getLoaded().keySet());
        } else {
            return super.processTabComplete(sender, args);
        }
    }
}
