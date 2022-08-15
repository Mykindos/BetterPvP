package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class DeleteBuildButton extends Button {

    private final GamerBuilds builds;
    private final Role role;
    private final int buildNumber;

    public DeleteBuildButton(GamerBuilds builds, Role role, int buildNumber, int slot) {
        super(slot, new ItemStack(Material.TNT), ChatColor.GREEN.toString() + ChatColor.BOLD + "Delete Build - " + buildNumber);
        this.builds = builds;
        this.role = role;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, buildNumber);
        roleBuildOptional.ifPresent(build -> {
            build.deleteBuild();
            UtilServer.callEvent(new DeleteBuildEvent(player, builds, build));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 0.6f);
        });
    }
}
