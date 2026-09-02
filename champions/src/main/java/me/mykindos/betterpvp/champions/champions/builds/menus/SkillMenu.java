package me.mykindos.betterpvp.champions.champions.builds.menus;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.SkillButton;
import me.mykindos.betterpvp.champions.champions.builds.menus.buttons.SkillTabButton;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.mykindos.betterpvp.core.utilities.Resources.Font.NEXO;

/**
 * The build editor. One skill slot is open at a time: the six tabs down the left and bottom choose which,
 * and the grid on the right lists every skill the class has for it.
 */
public class SkillMenu extends AbstractGui implements Windowed {

    /*
    Menu slots look like this:

    # # # # # # # # #  (0-8)
    h # # l # s s s s  (9-17)
    c # # b # s s s s  (18-26)
    # # # # # # # # #  (27-35)
    S # # B # # # # #  (36-44)
    # A a b g # # # <  (45-53)

    h/c/l/b: the armour the player is wearing, s: the skills of the open tab,
    S/A/a/b/g/B: the sword, axe, passive A, passive B, global and bow tabs, <: the back button
     */
    private static final int[] SKILL_SLOTS = {14, 15, 16, 17, 23, 24, 25, 26};
    private static final int BACK_SLOT = 53;
    private static final int[] TAB_SLOTS = {36, 45, 46, 47, 48, 39};
    private static final SkillType[] TABS = {SkillType.SWORD, SkillType.AXE, SkillType.PASSIVE_A,
            SkillType.PASSIVE_B, SkillType.GLOBAL, SkillType.BOW};

    private static final Map<EquipmentSlot, Integer> ARMOUR_SLOTS = Map.of(
            EquipmentSlot.HEAD, 9,
            EquipmentSlot.CHEST, 18,
            EquipmentSlot.LEGS, 12,
            EquipmentSlot.FEET, 21);

    /**
     * The slot label each armour piece takes its placeholder name from.
     */
    private static final Map<EquipmentSlot, String> ARMOUR_LABELS = Map.of(
            EquipmentSlot.HEAD, "helmet",
            EquipmentSlot.CHEST, "chestplate",
            EquipmentSlot.LEGS, "leggings",
            EquipmentSlot.FEET, "boots");

    @Getter
    private final RoleBuild roleBuild;
    private final BuildManager buildManager;
    private final Map<SkillType, List<Skill>> skills;
    @Nullable
    private final Windowed previous;

    /**
     * The slot the grid is currently listing. Every tab and grid item reads from this, so switching tabs
     * re-renders the menu rather than reopening it.
     */
    @Getter
    @Setter
    private SkillType selectedTab = SkillType.SWORD;

    /**
     * @param player       the player the menu is being opened for
     * @param builds       the builds of that player
     * @param role         the role the build belongs to
     * @param build        the one-indexed build id
     * @param buildManager the BuildManager, used to persist the build on close
     * @param skillManager the champions SkillManager
     * @param previous     the window the back button returns to. Null closes the menu instead
     * @param promptBuild  the optional build the player is being prompted to create. Null if empty
     */
    public SkillMenu(Player player, GamerBuilds builds, Role role, int build, BuildManager buildManager,
                     ChampionsSkillManager skillManager, @Nullable Windowed previous, @Nullable RoleBuild promptBuild) {
        super(9, 6);
        this.roleBuild = builds.getBuilds().stream().filter(b -> b.getRole() == role && b.getId() == build).findFirst().orElseThrow();
        this.buildManager = buildManager;
        this.previous = previous;
        this.skills = skillManager.getSkillsForRole(role).stream()
                .filter(skill -> skill.getType() != null)
                .filter(Skill::isEnabled)
                .sorted(Comparator.comparing(Skill::getName))
                .collect(Collectors.groupingBy(Skill::getType));

        ARMOUR_SLOTS.forEach((equipmentSlot, slot) -> setItem(slot, armourItem(player, role, equipmentSlot)));

        for (int index = 0; index < TAB_SLOTS.length; index++) {
            setItem(TAB_SLOTS[index], new SkillTabButton(TABS[index], roleBuild));
        }

        for (int index = 0; index < SKILL_SLOTS.length; index++) {
            setItem(SKILL_SLOTS[index], new SkillButton(index, roleBuild, promptBuild));
        }

        setItem(BACK_SLOT, new BackButton(previous, Key.key("betterpvp", "menu/icon/shadowed/cross_icon"), null));
    }

    /**
     * The skills this class can put into the given slot, in the order they are laid out in the grid.
     *
     * @param type the skill slot
     * @return the skills for that slot, empty if the class has none
     */
    public List<Skill> getSkills(SkillType type) {
        return skills.getOrDefault(type, List.of());
    }

    /**
     * The skill occupying a grid position for the open tab.
     *
     * @param index the position in the grid
     * @return the skill there, or null if the tab has fewer skills than that
     */
    @Nullable
    public Skill getSkill(int index) {
        final List<Skill> open = getSkills(selectedTab);
        return index < open.size() ? open.get(index) : null;
    }

    /**
     * The armour the player is actually wearing, so the build reads against their real kit. A piece that is
     * not the class's own stands in as the plain class piece instead.
     */
    private static SimpleItem armourItem(Player player, Role role, EquipmentSlot equipmentSlot) {
        final Material material = role.getMaterial(equipmentSlot);
        final ItemStack worn = player.getInventory().getItem(equipmentSlot);
        if (worn != null && worn.getType() == material) {
            return new SimpleItem(worn.clone());
        }

        final Component name = Translations.component("champions.menu.build.armor." + ARMOUR_LABELS.get(equipmentSlot),
                role.getDisplayName());
        return ItemView.builder()
                .material(material)
                .displayName(name.color(role.getColor()).decorate(TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build()
                .toSimpleItem();
    }

    @Override
    public Window show(@NonNull Player player) {
        final Window window = Windowed.super.show(player);
        window.addCloseHandler(() -> {
            buildManager.getBuildRepository().update(roleBuild);
        });
        return window;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.text("<shift:-38><glyph:menu_build_editor_0><shift:-1><glyph:menu_build_editor_1>").font(NEXO);
    }
}
