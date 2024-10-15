package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.ApplyBuildEvent;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EditBuildButton extends FlashingButton<BuildMenu> {

    private final Role role;
    private final int build;
    private final GamerBuilds builds;
    private final ChampionsSkillManager skillManager;
    private final RoleBuild roleBuild;
    private final Windowed previous;
    private final BuildManager buildManager;

    /**
     *
     * @param builds
     * @param role
     * @param build
     * @param roleBuild The optional rolebuild to prompt the player to create. Null if empty
     * @param buildManager
     * @param skillManager
     * @param previous
     */
    public EditBuildButton(GamerBuilds builds, Role role, int build, @Nullable RoleBuild roleBuild, BuildManager buildManager, ChampionsSkillManager skillManager, Windowed previous) {
        super();
        this.role = role;
        this.build = build;
        this.builds = builds;
        this.roleBuild = roleBuild;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.previous = previous;

        if (roleBuild != null) {
            if (roleBuild.getRole() == role && roleBuild.getId() == build) {
                this.setFlashing(true);
            }
        }
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
        });

        new SkillMenu(builds, role, build, buildManager, skillManager, previous, roleBuild).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(BuildMenu gui) {
        final Component standardComponent = Component.text("Edit Build " + build, NamedTextColor.GRAY);
        final Component flashComponent = Component.empty().append(Component.text("Click Me!", NamedTextColor.GREEN)).appendSpace().append(standardComponent);
        return ItemView.builder().material(Material.ANVIL)
                .displayName(this.isFlashing() ? flashComponent : standardComponent)
                .glow(this.isFlash())
                .build();
    }
}
