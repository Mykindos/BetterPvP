package me.mykindos.betterpvp.core.items;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.items.itemstack.BPvPCustomItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * Basic item data class, imported via database
 */
@Getter
@Setter
public class BPVPItem extends BPvPCustomItem {

    private final int maxDurability;
    private final boolean glowing;
    private final boolean giveUUID;

    public BPVPItem(String namespace, String key, Material material, Component name, List<Component> lore, int maxDurability, int customModelData, boolean glowing, boolean uuid) {
        super(namespace, key, material, name, lore, customModelData);
        this.maxDurability = maxDurability;
        this.glowing = glowing;
        this.giveUUID = uuid;
    }

}
