package me.mykindos.betterpvp.champions.champions.builds.menus;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.BuildButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

public class BuildMenu extends AbstractGui implements Windowed {

    private static final int[] BUILD_SLOTS = {11, 15, 29, 33};

    /**
     * The offset from a build's slot to where its extra button sits, one row below it.
     */
    private static final int EXTRA_BUTTON_OFFSET = 9;

    private static final int BACK_SLOT = 53;

    /**
     * A menu that shows the options to manage different builds for the specified role
     * @param builds The builds for the player looking at the menu
     * @param role The role that the builds are being managed for
     * @param buildManager The buildManager
     * @param skillManager The champions SkillManager
     * @param roleBuild The optional rolebuild to prompt the player to create. Null if empty
     * @param extraButtonProvider A function that provides an extra button for each build
     * @param previous the window the back button returns to. Null closes the menu instead
     */
    public BuildMenu(GamerBuilds builds, Role role, BuildManager buildManager, ChampionsSkillManager skillManager, @Nullable RoleBuild roleBuild, BiFunction<Integer, Windowed, Item> extraButtonProvider, @Nullable Windowed previous) {
        super(9, 6);

        for (int index = 0; index < BUILD_SLOTS.length; index++) {
            final int build = index + 1;
            setItem(BUILD_SLOTS[index], new BuildButton(builds, role, build, roleBuild, buildManager, skillManager));

            final Item extraButton = extraButtonProvider == null ? null : extraButtonProvider.apply(build, this);
            if (extraButton != null) {
                setItem(BUILD_SLOTS[index] + EXTRA_BUTTON_OFFSET, extraButton);
            }
        }

        setItem(BACK_SLOT, new BackButton(previous, Key.key("betterpvp", "menu/icon/shadowed/cross_icon"), null));
    }

    /**
     * A menu that shows the options to manage different builds for the specified role.
     * @param builds The builds for the player looking at the menu
     * @param role The role that the builds are being managed for
     * @param buildManager The buildManager
     * @param skillManager The champions SkillManager
     * @param roleBuild The optional rolebuild to prompt the player to create. Null if empty
     * @param previous the window the back button returns to. Null closes the menu instead
     */
    public BuildMenu(GamerBuilds builds, Role role, BuildManager buildManager, ChampionsSkillManager skillManager, @Nullable RoleBuild roleBuild, @Nullable Windowed previous) {
        this(builds, role, buildManager, skillManager, roleBuild, null, previous);
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("<shift:-38><glyph:menu_build_selector_0><shift:-1><glyph:menu_build_selector_1>").font(NEXO);
    }
}
