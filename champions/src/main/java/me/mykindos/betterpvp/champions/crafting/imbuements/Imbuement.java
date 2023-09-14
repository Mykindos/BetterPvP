package me.mykindos.betterpvp.champions.crafting.imbuements;

import lombok.Data;
import org.bukkit.Material;

@Data
public class Imbuement {

    private final String name;
    private final String key;
    private final String affixText;
    private final Material runeMaterial;
    private final double value;
    private final boolean canImbueArmour;
    private final boolean canImbueWeapons;
    private final boolean canImbueTools;

}
