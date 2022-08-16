package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ApplyBuildButton extends Button {

    private final GamerBuilds builds;
    private final Role role;
    private final int buildNumber;

    public ApplyBuildButton(GamerBuilds builds, Role role, int buildNumber, int slot, ItemStack item) {
        super(slot, item, ChatColor.GREEN.toString() + ChatColor.BOLD + "Apply Build - " + buildNumber);
        this.builds = builds;
        this.role = role;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, buildNumber);
        roleBuildOptional.ifPresent(build -> {
            RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
            activeBuild.setActive(false);

            build.setActive(true);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            UtilServer.callEvent(new ApplyBuildEvent(player, builds, activeBuild, build));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
        });
    }
}
