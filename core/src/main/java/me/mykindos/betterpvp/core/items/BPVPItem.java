package me.mykindos.betterpvp.core.items;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.items.itemstack.BPvPCustomItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

import java.util.List;

/**
 * Basic item data class, imported via database
 */
@Getter
@Setter
public class BPVPItem extends BPvPCustomItem {

    private final boolean glowing;
    private final boolean giveUUID;


    @Deprecated
    public BPVPItem(Material material, Component name, List<Component> lore, int customModelData, boolean glowing, boolean uuid) {
        this("betterpvp", PlainTextComponentSerializer.plainText().serialize(name), material, name, lore, customModelData, glowing, uuid);
    }

    public BPVPItem(String namespace, Material material, Component name, List<Component> lore, int customModelData, boolean glowing, boolean uuid) {
        this(namespace, PlainTextComponentSerializer.plainText().serialize(name).toLowerCase().replace(' ', '_'), material, name, lore, customModelData, glowing, uuid);
    }

    public BPVPItem(String namespace, String key, Material material, Component name, List<Component> lore, int customModelData, boolean glowing, boolean uuid) {
        super(namespace, key, material, name, lore, customModelData);
        this.glowing = glowing;
        this.giveUUID = uuid;
    }
}
