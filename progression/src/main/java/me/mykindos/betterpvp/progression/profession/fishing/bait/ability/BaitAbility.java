package me.mykindos.betterpvp.progression.profession.fishing.bait.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerThrowBaitEvent;
import me.mykindos.betterpvp.progression.profession.fishing.model.Bait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Base ability for all bait types.
 * Handles common functionality like throwing bait and applying effects.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class BaitAbility extends ItemAbility {

    protected double radius;
    protected double multiplier;
    protected long duration;
    
    private final CooldownManager cooldownManager;
    
    /**
     * Creates a new bait ability
     *
     * @param name The name of the ability
     * @param description The description of the ability
     * @param cooldownManager The cooldown manager
     */
    protected BaitAbility(String name, String description, CooldownManager cooldownManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Progression.class), name.toLowerCase().replace(" ", "_")), 
                name, description, TriggerTypes.RIGHT_CLICK);
        this.cooldownManager = cooldownManager;
    }
    
    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        // Check cooldown
        if (!cooldownManager.use(player, getName(), 5.0, true, true)) {
            return false;
        }
        
        // Remove the item from hand
        UtilInventory.remove(player, itemStack.getType(), 1);
        player.swingMainHand();
        
        // Create and throw the bait
        UtilServer.callEvent(new PlayerThrowBaitEvent(player, createBait()));
        
        // Send message
        final TextComponent name = Component.text(getName()).color(NamedTextColor.YELLOW);
        UtilMessage.message(player, "Bait", Component.text("You used ", NamedTextColor.GRAY).append(name));
        return true;
    }
    
    /**
     * Creates a new bait instance with the current settings
     *
     * @return The created bait
     */
    protected abstract Bait createBait();
} 