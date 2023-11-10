package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.skills.SkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public class ClassSelectionButton extends SimpleItem {

    private final BuildManager buildManager;
    private final Role role;
    private final SkillManager skillManager;
    private final Windowed parent;

    public ClassSelectionButton(BuildManager buildManager, SkillManager skillManager, Role role, Windowed parent) {
        super(ItemView.builder().material(role.getChestplate())
                .displayName(Component.text(role.getName(), role.getColor(), TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build());
        this.buildManager = buildManager;
        this.role = role;
        this.skillManager = skillManager;
        this.parent = parent;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();
        new BuildMenu(builds, role, buildManager, skillManager, parent).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }
}
