package me.mykindos.betterpvp.core.item.impl.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides enhanced mining speed to the Runed Pickaxe.
 * This ability is passive and does not need to be triggered.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnhancedMiningAbility extends ItemAbility {

    private double miningSpeed;

    public EnhancedMiningAbility() {
        super(new NamespacedKey(JavaPlugin.getPlugin(Core.class), "enhanced_mining"),
              "Enhanced Mining",
              "Grants enhanced mining speed for stone-based blocks. Works exactly like a pickaxe.",
              TriggerType.PASSIVE); // No trigger type since it's passive
        this.miningSpeed = 30.0; // Default value, will be overridden by config
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        // This is a passive ability and doesn't need active invocation
        return true;
    }
    
    /**
     * Applies the mining speed enhancement to the item.
     * Called during item initialization.
     *
     * @param itemStack The item to modify
     */
    public void applyMiningSpeed(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            ToolComponent toolComponent = meta.getTool();
            toolComponent.addRule(Tag.MINEABLE_PICKAXE, (float) miningSpeed, true);
            meta.setTool(toolComponent);
            itemStack.setItemMeta(meta);
        }
    }
} 