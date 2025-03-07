package me.mykindos.betterpvp.clans.fields.repository;

import com.google.common.base.Preconditions;
import lombok.Value;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;

/**
 * Represents a database entry for a Fields ore.
 */
@Value
public class FieldsBlockEntry {

    @Nullable
    FieldsInteractable type; // null if deleted
    String world;
    int x;
    int y;
    int z;
    String data;

    public FieldsBlock toFieldsOre() {
        Preconditions.checkNotNull(type, "type");
        return new FieldsBlock(world, x, y, z, data == null || data.isEmpty() ? null : Bukkit.createBlockData(data));
    }

}
