package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.MenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionButton extends Button {

    private final GamerBuilds builds;
    private final Role role;
    private final SkillManager skillManager;


    public ClassSelectionButton(GamerBuilds builds, Role role, SkillManager skillManager, int slot, ItemStack item) {
        super(slot, item, Component.text(role.getName(), role.getColor(), TextDecoration.BOLD));
        this.builds = builds;
        this.role = role;
        this.skillManager = skillManager;
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        MenuManager.openMenu(player, new BuildMenu(player, builds, role, skillManager));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
    }
}
