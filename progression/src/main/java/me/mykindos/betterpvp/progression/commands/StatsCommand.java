package me.mykindos.betterpvp.progression.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@Slf4j
public class StatsCommand extends Command {

    @Inject
    private ProgressionsManager progressionsManager;

    @Inject
    private ClientManager clientManager;

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Stats base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Stats", "Usage: <alt2>/stats <tree> [player]");
            return;
        }

        final Optional<ProgressionTree> treeOpt = progressionsManager.getTrees().stream()
                .filter(query -> query.getName().equalsIgnoreCase(args[0]))
                .findAny();

        if (treeOpt.isEmpty()) {
            UtilMessage.message(player, "Stats", "Progression tree not found [<alt2>%s</alt2>].", args[0]);
            return;
        }

        final ProgressionTree tree = treeOpt.get();
        if (args.length > 1) {
            clientManager.search(player).advancedOffline(args[1], result -> {
                run(player, result.iterator().next(), tree);
            });
        } else {
            run(player, client, tree);
        }
    }

    private void run(Player player, Client target, ProgressionTree tree) {
        final CompletableFuture<? extends ProgressionData<?>> loaded = tree.getStatsRepository().getDataAsync(target.getUniqueId());
        if (!loaded.isDone()) {
            UtilMessage.message(player, "Stats", "Retrieving player data...");
        }

        final String targetName = target.getName();
        loaded.whenComplete((data, throwable) -> {
            if (throwable != null) {
                UtilMessage.message(player, "Stats", "There was an error retrieving this player data.");
                log.error("There was an error retrieving player data for {}", targetName, throwable);
                return;
            }

            UtilMessage.message(player, "Stats", "Player data for <alt2>%s</alt2>:", targetName);
            UtilMessage.message(player, "Stats", "Level: <alt>%d", data.getLevel());
            for (Component component : data.getDescription()) {
                UtilMessage.message(player, "Stats", component);
            }
        });
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return progressionsManager.getTrees().stream()
                    .map(tree -> tree.getName().toLowerCase())
                    .toList();
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        return Collections.emptyList();
    }
}
