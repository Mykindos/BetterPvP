package me.mykindos.betterpvp.clans.champions.builds.menus.buttons;

import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ApplyBuildButton extends Button {

    private final Gamer gamer;
    private final Role role;
    private final int buildNumber;

    public ApplyBuildButton(Gamer gamer, Role role, int buildNumber, int slot, ItemStack item) {
        super(slot, item, ChatColor.GREEN.toString() + ChatColor.BOLD + "Apply Build - " + buildNumber);
        this.gamer = gamer;
        this.role = role;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        Optional<RoleBuild> roleBuildOptional = gamer.getBuild(role, buildNumber);
        roleBuildOptional.ifPresent(build -> {
            RoleBuild activeBuild = gamer.getActiveBuilds().get(role.getName());
            activeBuild.setActive(false);

            build.setActive(true);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            UtilServer.callEvent(new ApplyBuildEvent(player, gamer, activeBuild, build));
        });
    }
}
