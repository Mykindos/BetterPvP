package me.mykindos.betterpvp.champions.champions.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class PromptBuildCommand extends Command {

    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;
    private final ArmourManager armourManager;

    @Inject
    public PromptBuildCommand(BuildManager buildManager, ChampionsSkillManager championsSkillManager, ArmourManager armourManager) {
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
        this.armourManager = armourManager;
    }

    @Override
    public String getName() {
        return "promptbuild";
    }

    @Override
    public String getDescription() {
        return "Open a prompt build (for testing)";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Role role = Role.values()[UtilMath.randomInt(0, Role.values().length)];
        if (args.length > 0) {
            try {
                role = Role.valueOf(args[0]);
            } catch (IllegalArgumentException ignored) {

            }

        }
        RoleBuild promptBuild = buildManager.getRandomBuild(player, role, 4);
        new ClassSelectionMenu(buildManager, championsSkillManager, armourManager, promptBuild).show(player);
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? "ROLE" : ArgumentType.NONE.name();
    }

    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 0) return tabCompletions;

        String lowercaseArg = args[args.length - 1].toLowerCase();
        if (getArgumentType(args.length).equals("ROLE")) {
            tabCompletions.addAll(Arrays.stream(Role.values())
                    .map(role -> role.getName().toLowerCase())
                    .filter(name -> name.startsWith(lowercaseArg)).toList());
        }
        tabCompletions.addAll(super.processTabComplete(sender, args));
        return tabCompletions;
    }
}
