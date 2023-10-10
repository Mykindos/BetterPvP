package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class SkillsCommand extends Command {

    private final RoleManager roleManager;
    private final BuildManager buildManager;

    @Inject
    public SkillsCommand(RoleManager roleManager, BuildManager buildManager) {
        this.roleManager = roleManager;
        this.buildManager = buildManager;
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

                String sword = build.getSwordSkill() == null ? "" : build.getSwordSkill().getString();
                String axe = build.getAxeSkill() == null ? "" : build.getAxeSkill().getString();
                String bow = build.getBow() == null ? "" : build.getBow().getString();
                String passivea = build.getPassiveA() == null ? "" : build.getPassiveA().getString();
                String passiveb = build.getPassiveB() == null ? "" : build.getPassiveB().getString();
                String global = build.getGlobal() == null ? "" : build.getGlobal().getString();

                UtilMessage.message(player, Component.text("Sword: ", NamedTextColor.GREEN).append(Component.text(sword, NamedTextColor.WHITE)).appendNewline()
                        .append(Component.text("Axe: ", NamedTextColor.GREEN).append(Component.text(axe, NamedTextColor.WHITE))).appendNewline()
                        .append(Component.text("Bow: ", NamedTextColor.GREEN).append(Component.text(bow, NamedTextColor.WHITE))).appendNewline()
                        .append(Component.text("Passive A: ", NamedTextColor.GREEN).append(Component.text(passivea, NamedTextColor.WHITE))).appendNewline()
                        .append(Component.text("Passive B: ", NamedTextColor.GREEN).append(Component.text(passiveb, NamedTextColor.WHITE))).appendNewline()
                        .append(Component.text("Global: ", NamedTextColor.GREEN).append(Component.text(global, NamedTextColor.WHITE))));

            }
        }
    }
}
