package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public class EditBuildButton extends SimpleItem {

    private final Role role;
    private final int build;
    private final GamerBuilds builds;
    private final SkillManager skillManager;
    private final Windowed previous;
    private final BuildManager buildManager;

    public EditBuildButton(GamerBuilds builds, Role role, int build, BuildManager buildManager, SkillManager skillManager, Windowed previous) {
        super(ItemView.builder().material(Material.ANVIL)
                .displayName(Component.text("Edit Build " + build, NamedTextColor.GRAY))
                .build());
        this.role = role;
        this.build = build;
        this.builds = builds;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.previous = previous;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new SkillMenu(builds, role, build, buildManager, skillManager, previous).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
