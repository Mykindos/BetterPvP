package me.mykindos.betterpvp.progression.profession.fishing.bait;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.EventBaitAbility;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Event Bait item that provides special effects when fishing.
 */
@Singleton
@ItemKey("progression:event_bait")
public class EventBaitItem extends BaitItem {

    /**
     * Creates a new event bait item
     *
     * @param progression The progression instance
     * @param ability The event bait ability
     * @param loreRenderer The lore renderer
     */
    @Inject
    public EventBaitItem(Progression progression, EventBaitAbility ability) {
        super(progression, "Event Bait", new ItemStack(Material.LIGHT_BLUE_GLAZED_TERRACOTTA), ItemRarity.RARE, ability);
    }
} 