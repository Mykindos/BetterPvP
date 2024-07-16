package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class BuildCommand extends Command {

    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;
    private final ArmourManager armourManager;

    @Inject
    public BuildCommand(BuildManager buildManager, ChampionsSkillManager championsSkillManager, ArmourManager armourManager) {
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
        this.armourManager = armourManager;
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
        new ClassSelectionMenu(buildManager, championsSkillManager, armourManager, null).show(player);
    }
}
