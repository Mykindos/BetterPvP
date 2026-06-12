package me.mykindos.betterpvp.core.stats.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.menu.LeaderboardCategoryMenu;
import me.mykindos.betterpvp.core.stats.menu.LeaderboardMenu;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@CustomLog
public class LeaderboardCommand extends Command {

    private final LeaderboardManager leaderboards;

    @Inject
    public LeaderboardCommand(LeaderboardManager leaderboards) {
        this.leaderboards = leaderboards;
        aliases.add("lb");
        aliases.add("top");
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getDescription() {
        return "core.command.leaderboard.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            new LeaderboardCategoryMenu(leaderboards).show(player);
            return;
        }

        final String name = String.join(" ", args);
        final Optional<Leaderboard<?, ?>> leaderboardOpt = leaderboards.getViewableByName(name);
        if (leaderboardOpt.isEmpty() || !leaderboardOpt.get().isViewable()) {
            UtilMessage.message(player, "core.prefix.stats", "core.command.leaderboard.not_found", Component.text(name, NamedTextColor.YELLOW));
            return;
        }

        final Leaderboard<?, ?> leaderboard = leaderboardOpt.get();
        new LeaderboardMenu<>(player, leaderboard, null).show(player);
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
                return new ArrayList<>(leaderboards.getViewable().keySet().stream().filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList());
        }
        return Collections.emptyList();
    }
}
