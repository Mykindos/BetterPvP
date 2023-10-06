package me.mykindos.betterpvp.core.stats.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardRepository;
import me.mykindos.betterpvp.core.stats.menu.LeaderboardMenu;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class LeaderboardCommand extends Command {

    private final LeaderboardRepository leaderboards;

    @Inject
    public LeaderboardCommand(LeaderboardRepository leaderboards) {
        this.leaderboards = leaderboards;
        aliases.add("lb");
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getDescription() {
        return "Leaderboard base command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "Leaderboard", "Usage: <alt2>/leaderboard <name>");
            return;
        }

        final String name = String.join(" ", args);
        final Optional<Leaderboard<?, ?>> leaderboardOpt = leaderboards.getObject(name);
        if (leaderboardOpt.isEmpty()) {
            UtilMessage.message(player, "Leaderboard", "Leaderboard not found [<alt2>%s</alt2>].", name);
            return;
        }

        final Leaderboard<?, ?> leaderboard = leaderboardOpt.get();
        MenuManager.openMenu(player, new LeaderboardMenu(player, leaderboard));
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(leaderboards.getObjects().keySet());
        }
        return Collections.emptyList();
    }
}
