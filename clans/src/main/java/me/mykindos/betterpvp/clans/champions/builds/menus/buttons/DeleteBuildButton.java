package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.gamer.Gamer;
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

    private final Gamer gamer;
    private final Role role;
    private final int buildNumber;

    public DeleteBuildButton(Gamer gamer, Role role, int buildNumber, int slot) {
        super(slot, new ItemStack(Material.TNT), ChatColor.GREEN.toString() + ChatColor.BOLD + "Delete Build - " + buildNumber);
        this.gamer = gamer;
        this.role = role;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        Optional<RoleBuild> roleBuildOptional = gamer.getBuild(role, buildNumber);
        roleBuildOptional.ifPresent(build -> {
            build.deleteBuild();
            UtilServer.callEvent(new DeleteBuildEvent(player, gamer, build));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 0.6f);
        });
    }
}
