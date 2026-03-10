package me.mykindos.betterpvp.core.client.profile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.profile.menu.ProfileMenu;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class ProfileCommand extends Command {

    private final AchievementManager achievementManager;
    private final RealmManager realmManager;

    @Inject
    public ProfileCommand(AchievementManager achievementManager, RealmManager realmManager) {
        this.achievementManager = achievementManager;
        this.realmManager = realmManager;
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public String getDescription() {
        return "Opens a player profile";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new ProfileMenu(client, achievementManager, realmManager).show(player);
    }
}
