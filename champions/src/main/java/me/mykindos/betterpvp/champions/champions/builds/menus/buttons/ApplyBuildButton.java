package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ApplyBuildButton extends ControlItem<BuildMenu> {

    private final GamerBuilds builds;
    private final Role role;
    private final int build;

    public ApplyBuildButton(GamerBuilds builds, Role role, int build) {
        this.builds = builds;
        this.role = role;
        this.build = build;
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


        BuildSkill sword = null;
        BuildSkill axe = null;
        BuildSkill bow = null;
        BuildSkill passiveA = null;
        BuildSkill passiveB = null;
        BuildSkill global = null;

        Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, build);
        if (roleBuildOptional.isPresent()) {
            RoleBuild activeBuilds = roleBuildOptional.get();
            sword = activeBuilds.getSwordSkill();
            axe = activeBuilds.getAxeSkill();
            bow = activeBuilds.getBow();
            passiveA = activeBuilds.getPassiveA();
            passiveB = activeBuilds.getPassiveB();
            global = activeBuilds.getGlobal();
        }

        List<Component> lore = Arrays.asList(
                Component.text()
                        .append(Component.text("Sword: ", NamedTextColor.YELLOW))
                        .append(Component.text((sword != null ? sword.getString() : ""), NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("Axe: ", NamedTextColor.YELLOW))
                        .append(Component.text((axe != null ? axe.getString() : ""), NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("Bow: ", NamedTextColor.YELLOW))
                        .append(Component.text((bow != null ? bow.getString() : ""), NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("Passive A: ", NamedTextColor.YELLOW))
                        .append(Component.text((passiveA != null ? passiveA.getString() : ""), NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("Passive B: ", NamedTextColor.YELLOW))
                        .append(Component.text((passiveB != null ? passiveB.getString() : ""), NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("Global: ", NamedTextColor.YELLOW))
                        .append(Component.text((global != null ? global.getString() : ""), NamedTextColor.WHITE))
                        .build()
        );



        if (selected) {
            return ItemView.builder().displayName(buildName).material(type).lore(lore).glow(true).build();
        }

        return ItemView.builder().displayName(buildName).material(type).lore(lore).build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Optional<RoleBuild> roleBuildOptional = builds.getBuild(role, build);
        roleBuildOptional.ifPresent(selected -> {
            RoleBuild activeBuild = builds.getActiveBuilds().get(role.getName());
            activeBuild.setActive(false);

            selected.setActive(true);
            builds.getActiveBuilds().put(role.getName(), selected);

            UtilServer.callEvent(new ApplyBuildEvent(player, builds, activeBuild, selected));
            notifyWindows();
            getGui().updateControlItems();

            SoundEffect.HIGH_PITCH_PLING.play(player);
        });
    }
}
