package me.mykindos.betterpvp.core.items;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * Basic item data class, imported via database
 */
@Data
@AllArgsConstructor
public class BPVPItem {

    private final Material material;
    private final Component name;
    private final List<Component> lore;
    private final boolean glowing;

}
