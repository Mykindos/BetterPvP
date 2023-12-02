package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class SkillsCommand extends Command {
    @Inject
    @Config(path = "command/skills/seeOtherPlayerSkillsRank", defaultValue = "TRIAL_MOD" )
    private String seeOtherPlayerSkillsRankString;


    private final RoleManager roleManager;
    private final BuildManager buildManager;

    @Inject
    public SkillsCommand(RoleManager roleManager, BuildManager buildManager) {
        this.roleManager = roleManager;
        this.buildManager = buildManager;
        this.aliases.addAll(List.of("skill"));
    }

    @Override
    public String getName() {
        return "skills";
    }

    @Override
    public String getDescription() {
        return "View your current skills";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length > 0 && client.hasRank(Rank.valueOf(seeOtherPlayerSkillsRankString.toUpperCase()))) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> is not a valid player", args[0]));
                return;
            }
            Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(target.getUniqueId().toString());
            if (gamerBuildsOptional.isEmpty()) {
                UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> does not have any builds", target.getName()));
                return;
            }
            Optional<Role> roleOptional = roleManager.getObject(target.getUniqueId());
            if (roleOptional.isEmpty()) {
                UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow> does not have a set equipped", target.getName()));;
                return;
            }
            GamerBuilds builds = gamerBuildsOptional.get();
            Role role = roleOptional.get();
            RoleBuild build = builds.getActiveBuilds().get(role.getName());
            if (build != null) {
                UtilMessage.message(player, "Skills", UtilMessage.deserialize("<yellow>%s</yellow>'s Build:", target.getName()).appendNewline().append(build.getBuildComponent()));
                return;
            }

        }

        Optional<GamerBuilds> gamerBuildsOptional = buildManager.getObject(player.getUniqueId().toString());
        if (gamerBuildsOptional.isPresent()) {
            GamerBuilds builds = gamerBuildsOptional.get();

            Optional<Role> roleOptional = roleManager.getObject(player.getUniqueId());
            if (roleOptional.isEmpty()) {
                UtilMessage.message(player, "Skills", "You do not have a set equipped!");
                return;
            }

            Role role = roleOptional.get();
            RoleBuild build = builds.getActiveBuilds().get(role.getName());
            if (build != null) {
                UtilMessage.message(player, build.getBuildComponent());
            }
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
