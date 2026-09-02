package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.SkillMenu;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * One of the six skill slots along the edge of the build editor. It wears the icon of whatever skill the
 * build has in that slot, and clicking it lists that slot's skills in the grid.
 */
public class SkillTabButton extends ControlItem<SkillMenu> {

    /**
     * The icon each slot wears while the build has no skill in it.
     */
    private static final Map<SkillType, Key> PLACEHOLDERS = Map.of(
            SkillType.SWORD, placeholder("sword"),
            SkillType.AXE, placeholder("axe"),
            SkillType.BOW, placeholder("bow"),
            SkillType.PASSIVE_A, placeholder("passive_a"),
            SkillType.PASSIVE_B, placeholder("passive_b"),
            SkillType.GLOBAL, placeholder("global_passive"));

    private final SkillType type;
    private final RoleBuild roleBuild;

    public SkillTabButton(SkillType type, RoleBuild roleBuild) {
        this.type = type;
        this.roleBuild = roleBuild;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!ClickActions.LEFT.accepts(clickType) || getGui().getSkills(type).isEmpty()) {
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        if (getGui().getSelectedTab() == type) {
            return;
        }

        getGui().setSelectedTab(type);
        getGui().updateControlItems();
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(SkillMenu gui) {
        if (gui.getSkills(type).isEmpty()) {
            return unavailable();
        }

        final BuildSkill buildSkill = roleBuild.getBuildSkill(type);
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(buildSkill == null ? Key.key("betterpvp", "menu/gui/classes/empty_skill") : buildSkill.getSkill().getIcon())
                .displayName(Translations.component("champions.menu.skill.slot." + slotKey())
                        .color(NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD))
                .lore(lore(buildSkill))
                .action(ClickActions.LEFT, Translations.component("champions.menu.skill.slot.change"))
                .glow(gui.getSelectedTab() == type)
                .hideAdditionalTooltip(true)
                .frameLore(true)
                .build();
    }

    /**
     * A slot the class has no skills for at all, such as the bow on a class that cannot use one.
     */
    private ItemView unavailable() {
        final String key = type == SkillType.BOW
                ? "champions.menu.skill.slot.unavailable.bow"
                : "champions.menu.skill.slot.unavailable";
        return ItemView.builder()
                .material(Material.PAPER)
                .itemModel(Key.key("betterpvp", "menu/gui/classes/x_mark"))
                .displayName(Translations.component(key).color(NamedTextColor.RED))
                .hideAdditionalTooltip(true)
                .build();
    }

    private Component lore(BuildSkill buildSkill) {
        final Component selected = buildSkill == null
                ? Translations.component("champions.menu.skill.slot.none").color(NamedTextColor.GRAY)
                : buildSkill.getSkill().getDisplayName().color(NamedTextColor.GREEN);

        return Translations.component("champions.menu.skill.slot.selected", selected).color(NamedTextColor.GRAY);
    }

    private static Key placeholder(String name) {
        return Key.key("betterpvp", "menu/gui/classes/skills/" + name + "_placeholder");
    }

    private String slotKey() {
        return type.name().toLowerCase().replace("_", "-");
    }
}
