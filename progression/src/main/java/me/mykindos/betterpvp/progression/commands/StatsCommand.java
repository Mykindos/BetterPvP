package me.mykindos.betterpvp.progression.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.ProgressionsManager;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@Singleton
@Slf4j
public class StatsCommand extends Command {

    @Inject
    private ProgressionsManager progressionsManager;

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

        final ProgressionTree tree = progressionsManager.getTrees().stream()
                .filter(query -> query.getName().equalsIgnoreCase(args[0]))
                .findAny()
                .orElseThrow();

        OfflinePlayer target = player;
        if (args.length > 1) {
            target = Bukkit.getOfflinePlayer(args[1]);
        }

        if (target == null) {
            UtilMessage.message(player, "Stats", "Player not found [<alt2>%s</alt2>].", args[1]);
            return;
        }

        final CompletableFuture<? extends ProgressionData<?>> loaded = tree.getStatsRepository().getDataAsync(target);
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
    public boolean informInsufficientRank() {
        return true;
    }

}
