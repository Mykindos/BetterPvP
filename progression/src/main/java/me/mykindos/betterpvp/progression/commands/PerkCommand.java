package me.mykindos.betterpvp.progression.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.menu.PerksMenu;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@Slf4j
public class PerkCommand extends Command {

    @Inject
    private ProgressionsManager progressionsManager;


    @Override
    public String getName() {
        return "perks";
    }

    @Override
    public String getDescription() {
        return "Perks base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Perks", "Usage: <alt2>/stats <tree> [player]");
            return;
        }

        final Optional<ProgressionTree> treeOpt = progressionsManager.getTrees().stream()
                .filter(query -> query.getName().equalsIgnoreCase(args[0]))
                .findAny();

        if (treeOpt.isEmpty()) {
            UtilMessage.message(player, "Perks", "Progression tree not found [<alt2>%s</alt2>].", args[0]);
            return;
        }

        final ProgressionTree tree = treeOpt.get();
        run(player, tree);
    }


    private void run(Player player, ProgressionTree tree) {
        final CompletableFuture<? extends ProgressionData<?>> loaded = tree.getStatsRepository().getDataAsync(player.getUniqueId());
        if (!loaded.isDone()) {
            UtilMessage.message(player, "Perks", "Retrieving player data...");
        }

        loaded.whenComplete((data, throwable) -> {
            if (throwable != null) {
                UtilMessage.message(player, "Perks", "There was an error retrieving data.");
                log.error("There was an error retrieving player data for {}", player.getName(), throwable);
                return;
            }
            PerksMenu perksMenu = new PerksMenu(9, 2, player, tree, data);
            perksMenu.show(player);
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return progressionsManager.getTrees().stream()
                    .map(tree -> tree.getName().toLowerCase())
                    .toList();
        }
        return Collections.emptyList();
    }
}
