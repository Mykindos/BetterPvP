package me.mykindos.betterpvp.progression.tree.fishing.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.tree.fishing.Fishing;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

@Singleton
@SubCommand(FishingCommand.class)
public class FishingGiveRodSubCommand extends Command {

    @Inject
    private Fishing fishing;

    @Override
    public String getName() {
        return "giverod";
    }

    @Override
    public String getDescription() {
        return "Get fishing rods for fishing";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Fishing", "Please specify a rod type.");
            return;
        }

        StringBuilder str = new StringBuilder();
        for (String arg : args) {
            str.append(arg).append(" ");
        }
        String rodType = str.toString().trim();

        fishing.getRodTypes().stream().filter(rod -> rod.getName().equalsIgnoreCase(rodType)).findFirst().ifPresentOrElse(rod -> {
            final PlayerInventory inventory = player.getInventory();
            if (inventory.firstEmpty() == -1) {
                UtilMessage.message(player, "Fishing", "<red>Your inventory is full.");
                return;
            }

            inventory.addItem(rod.getPlayerFriendlyItem());
            UtilMessage.message(player, "Fishing", "You have been given a <alt2>" + rod.getName() + "</alt2>.");
        }, () -> UtilMessage.message(player, "Fishing", "Invalid rod type."));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return fishing.getRodTypes().stream().map(rod -> rod.getName().toLowerCase()).toList();
        }
        return new ArrayList<>();
    }
}
