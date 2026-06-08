package me.mykindos.betterpvp.core.repair;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.DescriptionComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Placeable stonecutter used to break repairable gear down into matching-tier
 * {@link me.mykindos.betterpvp.core.item.component.impl.repair.ReinforcementComponent
 * Reinforcement} catalysts. The actual interaction is handled by
 * {@link SalvageStationListener} — this class only defines the inventory item and
 * the fallback block material that the listener watches for.
 */
@ItemKey("core:salvage_station")
@Singleton
@FallbackItem(Material.STONECUTTER)
public class SalvageStation extends BaseItem {

    @Inject
    public SalvageStation() {
        super(translatableName("core.item.salvage-station.name"), ItemStack.of(Material.STONECUTTER), ItemGroup.BLOCK, ItemRarity.UNCOMMON);
        addBaseComponent(DescriptionComponent.translatable(1, "core.item.salvage-station.lore"));
    }
}
