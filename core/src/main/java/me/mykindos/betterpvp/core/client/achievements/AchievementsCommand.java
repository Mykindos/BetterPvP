package me.mykindos.betterpvp.core.client.achievements;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class AchievementsCommand extends Command implements IConsoleCommand {
    private final AchievementManager achievementManager;

    @Inject
    public AchievementsCommand(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }

    @Override
    public String getName() {
        return "achievements";
    }

    @Override
    public String getDescription() {
        return "Show achievements";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(achievementManager.getObjects().toString());
    }
}
