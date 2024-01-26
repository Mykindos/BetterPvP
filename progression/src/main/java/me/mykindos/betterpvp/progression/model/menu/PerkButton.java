package me.mykindos.betterpvp.progression.model.menu;

import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public class PerkButton extends SimpleItem {
    ProgressionPerk perk;

    PerkButton (ProgressionPerk perk) {
        super(new ItemStack(Material.PAPER));
    }
}
