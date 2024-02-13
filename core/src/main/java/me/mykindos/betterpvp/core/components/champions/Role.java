package me.mykindos.betterpvp.core.components.champions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

@AllArgsConstructor
@Getter
public enum Role {

    ASSASSIN("Assassin", "Assassin is a quick and agile class. With relatively low health and strong counters, this is not an easy class to be caught out in. Primarily suited for attacking distracted or otherwise occupied enemies, it will struggle against enemies that are prepared.",
            TextColor.color(224, 112, 0), Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
    KNIGHT("Knight", "Knight is a strong, aggressive class. It thrives being on the attack, having options to increase damage, while also having a few that can keep them alive long enough to kill their opponent.",
            TextColor.color(227, 227, 227), Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS),
    BRUTE("Brute", "Brute is a powerhouse that is rarely moved by others. It has good crowd control and defensive abilities.",
            TextColor.color(112, 255, 241), Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
    RANGER("Ranger", "Ranger is a ranged class skilled in the use of the bow. There are different options, rewarding precision, patience, or long distance accuracy.",
            TextColor.color(148, 148, 148), Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
    MAGE("Mage", "Mage is a class skilled with different elements. Choose between life, ice, fire, and earth to support your teammates, trap the enemy, and kill them.",
            TextColor.color(255, 237, 69), Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS),
    WARLOCK("Warlock", "Warlock is a class focused on health. Some abilities require the sacrifice in health, others punish enemies for proximity and low health.",
            TextColor.color(117, 117, 117), Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);

    private final String name;
    private final String description;
    private final TextColor color;
    private final Material helmet;
    private final Material chestplate;
    private final Material leggings;
    private final Material boots;

    public String getPrefix() {
        return name.substring(0, 1);
    }

    public String getName() {
        return name;
    }
}
