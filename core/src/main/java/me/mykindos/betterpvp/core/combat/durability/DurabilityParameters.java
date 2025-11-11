package me.mykindos.betterpvp.core.combat.durability;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Parameters controlling durability consumption during combat
 */
@Getter
@NoArgsConstructor
public class DurabilityParameters {
    
    /**
     * Whether to damage the attacker's weapon
     */
    private boolean damageAttackerWeapon = true;
    
    /**
     * Whether to damage the defender's armor
     */
    private boolean damageDefenderArmor = true;
    
    /**
     * Amount of durability to consume from attacker's weapon
     */
    private int attackerWeaponDamage = 1;
    
    /**
     * Amount of durability to consume from defender's armor pieces
     */
    private int defenderArmorDamage = 1;
    
    /**
     * Disables all durability consumption
     */
    public void disableAllDurability() {
        damageAttackerWeapon = false;
        damageDefenderArmor = false;
    }
    
    /**
     * Disables durability consumption for the defender only
     */
    public void disableDefenderDurability() {
        damageDefenderArmor = false;
    }
    
    /**
     * Disables durability consumption for the attacker only
     */
    public void disableAttackerDurability() {
        damageAttackerWeapon = false;
    }
    
    /**
     * Sets custom durability damage amounts for both attacker and defender
     * @param attackerDamage durability damage for attacker's weapon
     * @param defenderDamage durability damage for defender's armor
     */
    public void setDurabilityDamage(int attackerDamage, int defenderDamage) {
        this.attackerWeaponDamage = attackerDamage;
        this.defenderArmorDamage = defenderDamage;
    }
}
