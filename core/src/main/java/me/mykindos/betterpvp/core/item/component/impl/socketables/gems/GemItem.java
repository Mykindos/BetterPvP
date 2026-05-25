package me.mykindos.betterpvp.core.item.component.impl.socketables.gems;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableDescriptionComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableItem;
import org.bukkit.inventory.ItemStack;

@Getter
public abstract class GemItem extends SocketableItem {

    protected GemItem(Socketable socketable, ItemStack itemstack, ItemRarity rarity) {
        super(socketable,itemstack, ItemGroup.MATERIAL, rarity);
        addBaseComponent(new SocketableDescriptionComponent(socketable));
    }
}
