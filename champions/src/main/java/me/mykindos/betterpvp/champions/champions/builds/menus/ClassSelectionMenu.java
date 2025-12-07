package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ToggleShowPassiveButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Menu;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

public class ClassSelectionMenu extends AbstractGui implements Windowed {
    /**
     * The menu used to choose which role to manage builds for
     * @param buildManager the BuildManager
     * @param skillManager the ChampionsSkillManager
     * @param roleBuild The optional rolebuild to prompt the player to create. Null if empty
     */
    public ClassSelectionMenu(BuildManager buildManager, ChampionsSkillManager skillManager,
                              @Nullable RoleBuild roleBuild, boolean shouldShowPassives) {
        super(9, 3);

        /*
        Menu Slots looks like this:

        # # # # # # # # #  (0-8)
        # x x x # x x x #  (9-17)
        # # # # # # # # y  (18-26)
         */
        int[] slots = new int[] {10, 11, 12, 14, 15, 16};
        final Iterator<Role> iterator = Arrays.stream(Role.values()).iterator();
        for (int slot : slots) {
            final Role role = iterator.next();
            setItem(slot, new ClassSelectionButton(buildManager, skillManager, role, roleBuild, this,
                    shouldShowPassives));
        }

        setItem(26, new ToggleShowPassiveButton(buildManager, skillManager, shouldShowPassives));

        setBackground(Menu.BACKGROUND_ITEM);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("Pick a Class");
    }
}
