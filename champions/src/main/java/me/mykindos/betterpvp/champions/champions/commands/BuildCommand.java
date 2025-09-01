package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class BuildCommand extends Command {

    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;

    @Inject
    public BuildCommand(BuildManager buildManager, ChampionsSkillManager championsSkillManager) {
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getDescription() {
        return "Open the build editor";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new ClassSelectionMenu(buildManager, championsSkillManager, null, false).show(player);
    }
}
