package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ApplyBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.DeleteBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.EditBuildButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.RandomBuildButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class BuildMenu extends AbstractGui implements Windowed {

    private final Role role;

    public BuildMenu(GamerBuilds builds, Role role, BuildManager buildManager, ChampionsSkillManager skillManager, Windowed previous) {
        super(9, 6);
        this.role = role;

        setItem(0, new BackButton(previous));
        setItem(9, new SimpleItem(getItemView(role.getHelmet(), role, " Helmet")));
        setItem(18, new SimpleItem(getItemView(role.getChestplate(), role, " Chestplate")));
        setItem(27, new SimpleItem(getItemView(role.getLeggings(), role, " Leggings")));
        setItem(36, new SimpleItem(getItemView(role.getBoots(), role, " Boots")));
        int slot = 11;
        for (int build = 1; build < 5; build++) {

            setItem(slot, new ApplyBuildButton(builds, role, build));
            setItem(slot + 9, new EditBuildButton(builds, role, build, buildManager, skillManager, this));
            setItem(slot + 18, new DeleteBuildButton(builds, role, build, buildManager, skillManager, this));
            setItem(slot + 27, new RandomBuildButton(builds, role, build, buildManager, skillManager, this));

            slot += 2;
        }

        setBackground(Menu.BACKGROUND_ITEM);
    }

    private static ItemView getItemView(Material material, Role role, String name) {
        return ItemView.builder()
                .material(material)
                .displayName(Component.text(role.getName() + name, role.getColor(), TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text(role.getName() + " Builds");
    }
}
