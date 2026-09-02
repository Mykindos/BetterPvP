package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.combat.RoleBowService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class BuildCommand extends Command {

    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;
    private final RoleManager roleManager;
    private final RoleBowService roleBowService;

    @Inject
    public BuildCommand(BuildManager buildManager, ChampionsSkillManager championsSkillManager,
                        RoleManager roleManager, RoleBowService roleBowService) {
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
        this.roleManager = roleManager;
        this.roleBowService = roleBowService;
    }

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getDescription() {
        return "champions.command.build.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new ClassSelectionMenu(player, buildManager, championsSkillManager, roleManager, roleBowService, null).show(player);
    }
}
