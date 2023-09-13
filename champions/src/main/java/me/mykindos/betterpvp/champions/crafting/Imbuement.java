package me.mykindos.betterpvp.champions.crafting;

import lombok.Data;
import org.bukkit.Material;

@Data
public class Imbuement {

    private final String name;
    private final String key;
    private final String affixText;
    private final Material runeMaterial;
    private final boolean canImbueArmour;
    private final boolean canImbueWeapons;
    private final boolean canImbueTools;

}
