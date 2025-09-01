package me.mykindos.betterpvp.core.combat.durability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Processes durability consumption for combat events
 */
@Singleton
@CustomLog
public class DurabilityProcessor {

    private final ItemFactory itemFactory;

    @Inject
    private DurabilityProcessor(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Processes durability consumption for a damage event
     * @param event the damage event to process
     */
    public void processDurability(DamageEvent event) {
        DurabilityParameters params = event.getDurabilityParameters();

        // Process attacker weapon durability
        if (event.getCause().getCategories().contains(DamageCauseCategory.MELEE)
                && params.isDamageAttackerWeapon()
                && event.getDamager() instanceof Player attacker) {
            processAttackerWeapon(attacker, params);
        }

        // Process defender armor durability
        if (params.isDamageDefenderArmor() && event.getDamagee() instanceof Player defender) {
            processDefenderArmor(defender, params);
        }
    }
    
    /**
     * Processes durability damage for the attacker's weapon
     * @param attacker the attacking player
     * @param params the durability parameters
     */
    private void processAttackerWeapon(Player attacker, DurabilityParameters params) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        
        if (weapon.getType().isAir()) {
            return;
        }
        
        // Only damage weapons that can actually take durability damage
        if (canTakeDurabilityDamage(weapon)) {
            UtilItem.damageItem(attacker, weapon, params.getAttackerWeaponDamage());
            log.debug("Applied {} durability damage to attacker weapon: {}", 
                     params.getAttackerWeaponDamage(), weapon.getType()).submit();
        }
    }
    
    /**
     * Processes durability damage for the defender's armor
     * @param defender the defending player
     * @param params the durability parameters
     */
    private void processDefenderArmor(Player defender, DurabilityParameters params) {
        ItemStack[] armorContents = defender.getEquipment().getArmorContents();
        
        for (ItemStack armor : armorContents) {
            if (armor == null || armor.getType() == Material.AIR) {
                continue;
            }
            
            // Only damage armor that can actually take durability damage
            if (canTakeDurabilityDamage(armor)) {
                UtilItem.damageItem(defender, armor, params.getDefenderArmorDamage());
                log.debug("Applied {} durability damage to defender armor: {}", 
                         params.getDefenderArmorDamage(), armor.getType()).submit();
            }
        }
    }
    
    /**
     * Checks if an item can take durability damage
     * @param item the item to check
     * @return true if the item can take durability damage
     */
    @SuppressWarnings("UnstableApiUsage")
    private boolean canTakeDurabilityDamage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check if the item has durability
        return item.hasData(DataComponentTypes.DAMAGE) && item.hasData(DataComponentTypes.MAX_DAMAGE);
    }
}
