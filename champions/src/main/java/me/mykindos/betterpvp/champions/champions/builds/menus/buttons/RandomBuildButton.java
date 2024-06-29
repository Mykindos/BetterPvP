package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

import java.util.List;

public class RandomBuildButton extends ControlItem<BuildMenu> {

    private final GamerBuilds builds;
    private final Role role;
    private final int build;
    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;
    private final Windowed parent;

    public RandomBuildButton(GamerBuilds builds, Role role, int build, BuildManager buildManager, ChampionsSkillManager championsSkillManager, Windowed parent) {
        this.builds = builds;
        this.role = role;
        this.build = build;
        this.buildManager = buildManager;
        this.championsSkillManager = championsSkillManager;
        this.parent = parent;
    }

    @Override
    public ItemProvider getItemProvider(BuildMenu gui) {
        Material type = Material.COMPARATOR;
        Component buildName = Component.text("Random Build " + build, NamedTextColor.GRAY);
        List<Component> lore = List.of(UtilMessage.deserialize("Delete this build and generate a random one"));
        return ItemView.builder().displayName(buildName).lore(lore).material(type).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new ConfirmationMenu("Are you sure you want to replace this build with a random build?", success -> {
            if (Boolean.TRUE.equals(success)) {
                RoleBuild randomRoleBuild = buildManager.generateRandomBuild(player, role, build);
                RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
                activeBuild.setActive(false);

                randomRoleBuild.setActive(true);
                builds.getActiveBuilds().put(role.getName(), randomRoleBuild);

                UtilServer.callEvent(new ApplyBuildEvent(player, builds, activeBuild, randomRoleBuild));
                notifyWindows();
                getGui().updateControlItems();
            }
            new BuildMenu(builds, role, buildManager, championsSkillManager, parent).show(player);
        }).show(player);
    }
}
