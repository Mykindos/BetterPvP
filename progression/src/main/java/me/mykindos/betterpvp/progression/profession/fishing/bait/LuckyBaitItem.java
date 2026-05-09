package me.mykindos.betterpvp.progression.profession.fishing.bait;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.access.RestrictedAccessComponent;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.bait.ability.LuckyBaitAbility;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Lucky Bait item that increases treasure chance.
 */
@Singleton
@ItemKey("progression:lucky_bait")
public class LuckyBaitItem extends BaitItem {

    /**
     * Creates a new lucky bait item
     *
     * @param progression The progression plugin
     * @param ability The lucky bait ability
     */
    @Inject
    public LuckyBaitItem(Progression progression, LuckyBaitAbility ability) {
        super(progression, "Lucky Bait", new ItemStack(Material.YELLOW_GLAZED_TERRACOTTA), ItemRarity.EPIC, ability);
        addBaseComponent(new RestrictedAccessComponent(Set.of(AccessScope.CRAFT)));
    }
} 