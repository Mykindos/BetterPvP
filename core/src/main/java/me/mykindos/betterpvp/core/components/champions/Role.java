package me.mykindos.betterpvp.core.components.champions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

@AllArgsConstructor
@Getter
public enum Role {

    ASSASSIN("Assassin", TextColor.color(224, 112, 0), Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
    KNIGHT("Knight", TextColor.color(227, 227, 227), Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS),
    BRUTE("Brute", TextColor.color(112, 255, 241), Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
    RANGER("Ranger", TextColor.color(148, 148, 148), Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
    MAGE("Mage", TextColor.color(255, 237, 69), Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS),
    WARLOCK("Warlock", TextColor.color(117, 117, 117), Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);

    private final String name;
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
