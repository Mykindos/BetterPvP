package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.combat.RoleBowService;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

/**
 * Flags that the selected class is handed a bow, what its arrows land for, and whether it will fire at all
 * without a bow skill prepared. The slot is empty for a class that cannot use one, rather than reading out
 * an absence.
 */
public class ClassBowButton extends ControlItem<ClassSelectionMenu> {

    private static final int DESCRIPTION_WIDTH = 35;

    private final RoleBowService roleBowService;

    public ClassBowButton(RoleBowService roleBowService) {
        this.roleBowService = roleBowService;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Purely a readout
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        final Role role = gui.getSelected();
        if (!role.isUsesBow()) {
            return ItemProvider.EMPTY;
        }

        final ItemView.ItemViewBuilder builder = ItemView.builder()
                .material(Material.BOW)
                .displayName(Translations.component("champions.menu.class.bow").color(NamedTextColor.YELLOW))
                .lore(Component.empty())
                .lore(Translations.component("champions.menu.class.bow.damage").color(NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(UtilFormat.formatNumber(roleBowService.getArrowDamage(role)), NamedTextColor.DARK_RED))
                        .append(Component.space())
                        .append(Component.text('❤', NamedTextColor.DARK_RED)))
                .lore(Component.empty())
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true);

        if (roleBowService.isOnlyWhilePrepared(role)) {
            builder.lore(ComponentWrapper.markForWrap(
                    Translations.component("champions.menu.class.bow.skills-only").color(NamedTextColor.GRAY),
                    DESCRIPTION_WIDTH));
        }

        return builder.build();
    }
}
