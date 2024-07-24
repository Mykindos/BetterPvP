package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.DeleteBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DeleteBuildButton extends SimpleItem {

    private final GamerBuilds builds;
    private final Role role;
    private final int buildNumber;
    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;
    private final Windowed parent;

    public DeleteBuildButton(GamerBuilds builds, Role role, int build, BuildManager buildManager, ChampionsSkillManager skillManager, Windowed parent) {
        super(ItemView.builder().material(Material.RED_CONCRETE)
                .displayName(Component.text("Delete Build " + build , NamedTextColor.RED))
                .build());
        this.builds = builds;
        this.role = role;
        this.buildNumber = build;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.parent = parent;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new ConfirmationMenu("Are you sure you want to delete this build?", success -> {
            if (success) {
                Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, buildNumber);
                roleBuildOptional.ifPresent(build -> {
                    build.deleteBuild();
                    UtilServer.callEvent(new DeleteBuildEvent(player, builds, build));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 0.6f);
                });
            }
            new BuildMenu(builds, role, buildManager, skillManager, parent).show(player);
        }).show(player);
    }
}
