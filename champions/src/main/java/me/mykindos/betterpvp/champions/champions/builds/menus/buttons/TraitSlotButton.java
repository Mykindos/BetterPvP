package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.builds.menus.TraitItems;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.traits.Trait;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * One of the fixed trait slots in the class selector. A class with fewer traits than there are slots leaves
 * the remainder showing as empty rather than collapsing them, so the row keeps its shape between classes.
 */
public class TraitSlotButton extends ControlItem<ClassSelectionMenu> {

    private final ChampionsSkillManager skillManager;
    private final int index;

    public TraitSlotButton(ChampionsSkillManager skillManager, int index) {
        this.skillManager = skillManager;
        this.index = index;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Purely a readout
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        final List<Trait> traits = skillManager.getTraitsForRole(gui.getSelected());
        if (index >= traits.size()) {
            return TraitItems.empty();
        }

        return TraitItems.of(traits.get(index));
    }
}
