package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.BuildEditorButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassBowButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassHealthButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.ClassStatButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.TraitSlotButton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.combat.RoleBowService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

/**
 * The menu used to choose which class to inspect and manage builds for.
 * <p>
 * The health and bow readouts, the traits, the three stat bands and the build editor all read from
 * {@link #selected}, so picking a class re-renders the whole menu without reopening it.
 */
public class ClassSelectionMenu extends AbstractGui implements Windowed {

    private static final int[] CLASS_SLOTS = {10, 11, 19, 20, 28, 29};
    private static final int HEALTH_SLOT = 5;
    private static final int BOW_SLOT = 8;
    private static final int[] DAMAGE_SLOTS = {23, 24, 25, 26};
    private static final int[] SUPPORT_SLOTS = {32, 33, 34, 35};
    private static final int[] MOBILITY_SLOTS = {41, 42, 43, 44};
    private static final int[] BUILD_EDITOR_SLOTS = {37, 38, 46, 47};
    private static final int[] TRAIT_SLOTS = {50, 51, 52, 53};

    /**
     * The slot within each stat band that is modelled as the bar; the rest of the band is invisible backing.
     * The bar spans the band's last three slots, so it is centred on the third — the first sits over the stat
     * icon painted into the menu background.
     */
    private static final int BAR_SLOT = 2;

    @Getter
    @Setter
    private Role selected;

    /**
     * @param player       the player the menu is being opened for
     * @param buildManager the BuildManager
     * @param skillManager the ChampionsSkillManager
     * @param roleManager  the RoleManager, used to default the selection to the class the player is wearing
     * @param roleBowService the RoleBowService, read for the bow readout
     * @param promptBuild  The optional rolebuild to prompt the player to create. Null if empty
     */
    public ClassSelectionMenu(Player player, BuildManager buildManager, ChampionsSkillManager skillManager,
                              RoleManager roleManager, RoleBowService roleBowService,
                              @Nullable RoleBuild promptBuild) {
        super(9, 6);

        this.selected = promptBuild != null ? promptBuild.getRole() : roleManager.getRole(player)
                .or(() -> roleManager.getLastEquippedRole(player))
                .orElse(Role.DEFAULT);

        final Role[] roles = Role.values();
        for (int index = 0; index < CLASS_SLOTS.length && index < roles.length; index++) {
            setItem(CLASS_SLOTS[index], new ClassSelectionButton(roles[index]));
        }

        setItem(HEALTH_SLOT, new ClassHealthButton());
        setItem(BOW_SLOT, new ClassBowButton(roleBowService));

        setStatBand(DAMAGE_SLOTS, ClassStat.DAMAGE);
        setStatBand(SUPPORT_SLOTS, ClassStat.SUPPORT);
        setStatBand(MOBILITY_SLOTS, ClassStat.MOBILITY);

        for (int slot : BUILD_EDITOR_SLOTS) {
            setItem(slot, new BuildEditorButton(buildManager, skillManager, promptBuild));
        }

        for (int index = 0; index < TRAIT_SLOTS.length; index++) {
            setItem(TRAIT_SLOTS[index], new TraitSlotButton(skillManager, index));
        }
    }

    private void setStatBand(int[] slots, ClassStat stat) {
        for (int index = 0; index < slots.length; index++) {
            setItem(slots[index], new ClassStatButton(stat, index == BAR_SLOT));
        }
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("<shift:-38><glyph:menu_class_selector_0><shift:-1><glyph:menu_class_selector_1>").font(NEXO);
    }
}
