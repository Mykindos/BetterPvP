package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RandomBuild;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
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

public class RandomBuildButton extends ControlItem<BuildMenu> {

    private final GamerBuilds builds;
    private final Role role;
    private final int build;
    private final BuildManager buildManager;
    private final ChampionsSkillManager championsSkillManager;

    public RandomBuildButton(GamerBuilds builds, Role role, int build, BuildManager buildManager, ChampionsSkillManager championsSkillManager, BuildManager buildManager1, ChampionsSkillManager championsSkillManager1) {
        this.builds = builds;
        this.role = role;
        this.build = build;
        this.buildManager = buildManager1;
        this.championsSkillManager = championsSkillManager1;
    }

    @Override
    public ItemProvider getItemProvider(BuildMenu gui) {
        Material type = switch (build) {
            case 1 -> Material.RED_DYE;
            case 2 -> Material.ORANGE_DYE;
            case 3 -> Material.YELLOW_DYE;
            case 4 -> Material.LIME_DYE;
            default -> throw new IllegalStateException("Unexpected value: " + build);
        };

        boolean selected = builds.getActiveBuilds().get(role.getName()).getId() == build;
        Component buildName = Component.text("Apply Build " + build, NamedTextColor.GRAY);
        if (selected) {
            buildName = Component.text("\u00BB Build " + build + " \u00AB", NamedTextColor.GREEN);
        }

        if (selected) {
            return ItemView.builder().displayName(buildName).material(type).glow(true).build();
        }

        return ItemView.builder().displayName(buildName).material(type).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        RoleBuild randomRoleBuild = RandomBuild.getRandomBuild(player, role, build, buildManager, championsSkillManager);
        RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
        activeBuild.setActive(false);

        randomRoleBuild.setActive(true);
        builds.getActiveBuilds().put(role.getName(), randomRoleBuild);

        UtilServer.callEvent(new ApplyBuildEvent(player, builds, activeBuild, randomRoleBuild));
        notifyWindows();
        getGui().updateControlItems();

        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
