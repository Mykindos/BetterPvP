package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class SendSkillsCommand extends Command {

    private final Champions champions;
    private final RoleManager roleManager;
    private final BuildManager buildManager;

    private final CooldownManager cooldownManager;

    @Inject
    public SendSkillsCommand(Champions champions, RoleManager roleManager, BuildManager buildManager, CooldownManager cooldownManager) {
        this.champions = champions;
        this.roleManager = roleManager;
        this.buildManager = buildManager;
        this.cooldownManager = cooldownManager;
        this.aliases.addAll(List.of("sendskill", "sendbuild"));
    }

    @Override
    public String getName() {
        return "sendskills";
    }

    @Override
    public String getDescription() {
        return "Send your skills as a formatted message in your chat channel";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!cooldownManager.use(player, getName(), 15d, false, true)){
            UtilMessage.message(player, "Skills", "You must wait some time between using this command again");
            return;
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
                Component messageComponent = UtilMessage.deserialize("<white>I am currently running <green>%s</green>:", role.getName()).decoration(TextDecoration.BOLD, false)
                        .appendNewline().append(build.getBuildComponent());
                UtilServer.runTaskAsync(champions, () -> UtilServer.callEvent(new ChatSentEvent(player, Bukkit.getOnlinePlayers(), UtilMessage.deserialize("<yellow>%s:</yellow>"), messageComponent)));
            }
        }
    }
}
