package me.mykindos.betterpvp.progression.profession.fishing.bait;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.SpeedyBaitAbility;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Speedy Bait item that increases fishing speed.
 */
@Singleton
public class SpeedyBaitItem extends BaitItem {

    /**
     * Creates a new speedy bait item
     *
     * @param ability The speedy bait ability
     * @param loreRenderer The lore renderer
     */
    @Inject
    public SpeedyBaitItem(Progression progression, SpeedyBaitAbility ability) {
        super(progression, "Speedy Bait", new ItemStack(Material.ORANGE_GLAZED_TERRACOTTA), ItemRarity.UNCOMMON, ability);
    }
} 