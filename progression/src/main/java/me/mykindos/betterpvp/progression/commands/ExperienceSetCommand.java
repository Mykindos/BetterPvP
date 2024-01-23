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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class ExperienceSetCommand extends Command {
    @Inject
    private ProgressionsManager progressionsManager;

    @Inject
    private ClientManager clientManager;

    @Override
    public String getName() {
        return "pexpset";
    }

    @Override
    public String getDescription() {
        return "Progression update experience for player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "Progression", "Usage: <alt2>/pexpset <tree> <player> <amount>");
            return;
        }

        final Optional<ProgressionTree> treeOpt = progressionsManager.getTrees().stream()
                .filter(query -> query.getName().equalsIgnoreCase(args[0]))
                .findAny();

        if (treeOpt.isEmpty()) {
            UtilMessage.message(player, "Progression", "Progression tree not found [<alt2>%s</alt2>].", args[0]);
            return;
        }

        final ProgressionTree tree = treeOpt.get();
        Optional<Client> clientOptional = clientManager.search(player).online(args[1]);
        if (clientOptional.isEmpty()) {
            UtilMessage.message(player, "Progression", UtilMessage.deserialize("<green>%s</green> is not a valid player", args[1]));
            return;
        }

        long newExperience;
        try {
            newExperience = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "Progression", UtilMessage.deserialize("<green>%s</green> is not a valid long", args[2]));
            return;
        }

        Client targetClient = clientOptional.get();
        tree.getStatsRepository().getDataAsync(client.getUniqueId()).whenComplete((progressionData, throwable) -> {
            progressionData.setExperience(newExperience);
            UtilMessage.message(player, "Progression", UtilMessage.deserialize("Set <green>%s</green>'s <yellow>%s</yellow> exp to <green>%s</green>", client.getName(), tree.getName(), newExperience));
        }).exceptionally(throwable -> {
            log.error("Failed retrieving progressionData for " + targetClient.getName());
            return null;
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
