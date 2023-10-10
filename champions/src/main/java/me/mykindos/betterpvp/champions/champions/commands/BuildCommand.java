package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class BuildCommand extends Command {

    private final BuildManager buildManager;
    private final SkillManager skillManager;

    @Inject
    public BuildCommand(BuildManager buildManager, SkillManager skillManager) {
        this.buildManager = buildManager;
        this.skillManager = skillManager;
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
        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId());
        gamerBuildsOptional.ifPresent(builds -> MenuManager.openMenu(player, new ClassSelectionMenu(player, builds, skillManager)));
    }
}
