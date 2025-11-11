package me.mykindos.betterpvp.core.combat.click.events;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.utilities.Resources;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("UnstableApiUsage")
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@AllArgsConstructor
public class RightClickEvent extends CustomEvent {

    public static final ItemStack INVISIBLE_BLOCKING_ITEM = ItemStack.of(Material.PAPER);

    static {
        INVISIBLE_BLOCKING_ITEM.setData(DataComponentTypes.ITEM_MODEL, Resources.ItemModel.INVISIBLE);
        INVISIBLE_BLOCKING_ITEM.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(72_000)
                .hasConsumeParticles(false)
                .animation(ItemUseAnimation.NONE)
                .build());
    }


    private final Player player;
    private ItemStack blockingItem;
    private boolean isHoldClick;
    private EquipmentSlot hand;

    public boolean hasBlockingItem() {
        return blockingItem != null;
    }

}
