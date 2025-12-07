package me.mykindos.betterpvp.core.item.component.impl.durability;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a durability component for items, allowing them to have a maximum damage value
 * and track the current damage.
 * <br>
 * <b>This component is needed over the default durability component because it allows for us to
 * dynamically change this over time, and we don't have to backpropagate the damage value to
 * every existing item. Instead, this is calculated every time the player renders the item.</b>
 */
@Getter
@Setter
public class DurabilityComponent extends AbstractItemComponent {

    private int maxDamage;
    private int damage;

    public DurabilityComponent(int maxDamage) {
        super("durability");
        this.maxDamage = maxDamage;
    }

    public boolean isBroken() {
        return damage >= maxDamage;
    }

    public void setMaxDamage(int maxDamage) {
        Preconditions.checkArgument(maxDamage > 0, "Max damage must be greater than 0");
        this.maxDamage = maxDamage;
    }

    public void damage(Player player, ItemInstance itemInstance, int amount) {
        Preconditions.checkArgument(amount > 0, "Damage amount must be greater than 0");
        if (isBroken()) {
            return; // Item is already broken, no need to damage it further
        }

        UtilItem.damageItem(player, itemInstance, amount);
    }

    @Override
    public DurabilityComponent copy() {
        final DurabilityComponent durabilityComponent = new DurabilityComponent(maxDamage);
        durabilityComponent.setDamage(damage);
        return durabilityComponent;
    }
}
